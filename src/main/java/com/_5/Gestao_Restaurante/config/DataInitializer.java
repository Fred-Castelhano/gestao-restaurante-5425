package com._5.Gestao_Restaurante.config;

import com._5.Gestao_Restaurante.model.Utilizador;
import com._5.Gestao_Restaurante.Repository.UtilizadorRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initData(UtilizadorRepository utilizadorRepository) {
        return args -> {
            // Só insere se a tabela estiver vazia
            if (utilizadorRepository.count() == 0) {
                Utilizador admin = new Utilizador(0, "Administrador", "admin@restaurante.com", "1234", "Admin");
                utilizadorRepository.save(admin);
                System.out.println("> Utilizador Administrador criado com sucesso!");
            }
        };
    }
}