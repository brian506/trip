package com.trip.supplier.global;

import com.trip.supplier.Supplier;
import com.trip.supplier.SupplierClient;
import com.trip.supplier.exception.SupplierCallException;
import com.trip.supplier.exception.SupplierFailureType;
import com.trip.supplier.vo.SupplierDispatchResult;
import com.trip.supplier.vo.SupplierFailure;
import com.trip.supplier.vo.SupplierRoom;
import com.trip.supplier.vo.SupplierStayCodes;
import com.trip.support.exception.AppException;
import com.trip.support.exception.ErrorType;
import com.trip.support.vo.Guests;
import com.trip.support.vo.StayPeriod;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SupplierDispatcher {

    private static final String BUDGET_EXCEEDED = "BUDGET_EXCEEDED";
    private static final String UNEXPECTED = "UNEXPECTED";

    private final List<SupplierClient> clients;
    private final SupplierProperties properties;

    public SupplierDispatchResult dispatch(StayPeriod period, Guests guests, Map<Supplier, Set<String>> stayCodesBySupplier) {
        List<SupplierClient> targets = clients.stream()
                .filter(client -> !stayCodesBySupplier.getOrDefault(client.supplier(), Set.of()).isEmpty())
                .toList();
        if (targets.isEmpty()) {
            return SupplierDispatchResult.empty();
        }

        Queue<SupplierRoom> rooms = new ConcurrentLinkedQueue<>();
        Map<Supplier, SupplierFailure> failures = new ConcurrentHashMap<>();

        List<Callable<Void>> tasks = targets.stream()
                .map(client -> toTask(client, period, guests, stayCodesBySupplier.get(client.supplier()), rooms, failures))
                .toList();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Void>> futures = executor.invokeAll(tasks, properties.searchBudget().toMillis(), TimeUnit.MILLISECONDS);
            markBudgetExceeded(targets, futures, failures);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AppException(ErrorType.DEFAULT_ERROR, e);
        }
        return new SupplierDispatchResult(List.copyOf(rooms), List.copyOf(failures.values()), targets.size());
    }

    private Callable<Void> toTask(SupplierClient client, StayPeriod period, Guests guests, Set<String> stayCodes,
                                  Queue<SupplierRoom> rooms, Map<Supplier, SupplierFailure> failures) {
        return () -> {
            fetchBatches(client, period, guests, stayCodes, rooms, failures);
            return null;
        };
    }

    private void fetchBatches(SupplierClient client, StayPeriod period, Guests guests, Set<String> stayCodes,
                              Queue<SupplierRoom> rooms, Map<Supplier, SupplierFailure> failures) {
        for (SupplierStayCodes batch : SupplierStayCodes.partition(stayCodes)) {
            try {
                rooms.addAll(client.fetchRooms(period, guests, batch));
            } catch (SupplierCallException e) {
                // 실패는 공급사당 한 건이라 첫 건만 남긴다.
                failures.putIfAbsent(client.supplier(), e.toFailure());
                log.warn("[공급사 재고·요금 : 묶음 실패]: supplier={} | type={} | code={} | stayCodes={}",
                        e.getSupplier(), e.getType(), e.getCode(), batch.values().size());
                if (e.getType().stopsRemainingBatches()) {
                    return;
                }
            } catch (RuntimeException e) {
                // 공급사 응답이 아니라 우리 쪽 오류다. 남은 묶음도 같은 결과일 테니 멈춘다.
                failures.putIfAbsent(client.supplier(),
                        new SupplierFailure(client.supplier(), SupplierFailureType.MALFORMED, UNEXPECTED));
                log.error("[공급사 재고·요금 : 예기치 못한 오류]: supplier={}", client.supplier(), e);
                return;
            }
        }
    }

    private void markBudgetExceeded(List<SupplierClient> targets, List<Future<Void>> futures,
                                    Map<Supplier, SupplierFailure> failures) {
        for (int i = 0; i < futures.size(); i++) {
            if (!futures.get(i).isCancelled()) {
                continue;
            }
            Supplier supplier = targets.get(i).supplier();
            failures.putIfAbsent(supplier, new SupplierFailure(supplier, SupplierFailureType.UNAVAILABLE, BUDGET_EXCEEDED));
            log.warn("[공급사 재고·요금 : 예산 초과]: supplier={} | budget={}", supplier, properties.searchBudget());
        }
    }
}
