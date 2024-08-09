package com.app;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;
import org.springframework.context.ApplicationContext;

import javax.swing.*;
import java.nio.channels.Channel;

@SpringBootApplication
public class ChannelMetadataStoreApplication {

	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(ChannelMetadataStoreApplication.class);
		application.setApplicationStartup(new BufferingApplicationStartup(2048));
		application.run(args);
	}
}
