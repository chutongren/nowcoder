package com.nowcoder.community;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CommunityApplication {
//	但是es7以上就没有这个问题了
//	@PostConstruct
//	public void init() {
//		// 解决netty启动冲突问题
//		// see Netty4Utils.setAvailableProcessors()
//		System.setProperty("es.set.netty.runtime.available.processors", "false");
//	}

	public static void main(String[] args) {
		SpringApplication.run(CommunityApplication.class, args);
	}

}
