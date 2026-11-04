package com.mss301.brand;

import com.mss301.brand.entity.BrandWatch;
import com.mss301.brand.entity.WatchBrand;
import com.mss301.brand.repository.BrandRepository;
import com.mss301.brand.repository.BrandWatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final BrandRepository brandRepository;
    private final BrandWatchRepository brandWatchRepository;

    @Override
    public void run(String... args) {

        if (brandRepository.count() == 0) {

            List<WatchBrand> brands = List.of(

                    WatchBrand.builder()
                            .brandName("Rolex")
                            .countryOfOrigin("Switzerland")
                            .build(),

                    WatchBrand.builder()
                            .brandName("Omega")
                            .countryOfOrigin("Switzerland")
                            .build(),

                    WatchBrand.builder()
                            .brandName("Seiko")
                            .countryOfOrigin("Japan")
                            .build(),

                    WatchBrand.builder()
                            .brandName("Casio")
                            .countryOfOrigin("Japan")
                            .build(),

                    WatchBrand.builder()
                            .brandName("TAG Heuer")
                            .countryOfOrigin("Switzerland")
                            .build()
            );

            brandRepository.saveAll(brands);

            log.info(
                    "Seeded {} brands",
                    brands.size()
            );
        }

        if (brandWatchRepository.count() == 0) {

            Map<Integer, Integer> watchToBrand =
                    new LinkedHashMap<>();

            watchToBrand.put(1, 1);
            watchToBrand.put(2, 2);
            watchToBrand.put(3, 3);
            watchToBrand.put(4, 4);
            watchToBrand.put(5, 5);
            watchToBrand.put(6, 2);
            watchToBrand.put(7, 3);
            watchToBrand.put(8, 1);
            watchToBrand.put(9, 4);
            watchToBrand.put(10, 4);

            watchToBrand.forEach(
                    (watchId, brandId) ->
                            brandWatchRepository.save(
                                    BrandWatch.builder()
                                            .watchId(watchId)
                                            .brandId(brandId)
                                            .updatedAt(LocalDateTime.now())
                                            .build()
                            )
            );

            log.info(
                    "Seeded {} brand-watch links",
                    watchToBrand.size()
            );
        }
    }
}