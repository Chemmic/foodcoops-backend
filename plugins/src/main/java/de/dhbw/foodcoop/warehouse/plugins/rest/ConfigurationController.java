package de.dhbw.foodcoop.warehouse.plugins.rest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import de.dhbw.foodcoop.warehouse.application.admin.ConfigurationService;
import de.dhbw.foodcoop.warehouse.domain.entities.ConfigurationEntity;

@RestController
public class ConfigurationController {

	private final ConfigurationService service;

	public ConfigurationController(ConfigurationService service) {
		this.service = service;
	}

	@GetMapping("/configuration")
	public ConfigurationEntity getConfig() {
		return service.getConfig().orElseThrow();
	}

	@PutMapping("/configuration")
	public ConfigurationEntity updateConfig(
			@RequestBody ConfigurationEntity configuration) {

		return service.updateConfig(configuration);
	}
}