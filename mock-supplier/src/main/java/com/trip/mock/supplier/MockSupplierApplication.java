package com.trip.mock.supplier;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// 공급사 A·B를 흉내 내는 Mock 서버. 운영 앱과 별도 모듈·별도 프로세스(9090)로 띄운다.
@SpringBootApplication
public class MockSupplierApplication {

	public static void main(String[] args) {
		SpringApplication.run(MockSupplierApplication.class, args);
	}

}
