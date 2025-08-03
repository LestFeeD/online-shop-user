package com.shopir.user.service;

import com.shopir.user.dto.response.CityResponseDto;
import com.shopir.user.entity.City;
import com.shopir.user.factories.CityFactory;
import com.shopir.user.repository.CityRepository;
import com.shopir.user.repository.WebUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class  CityService {
    private final CityRepository cityRepository;
    private final CityFactory cityFactory;

    @Autowired
    public CityService(CityRepository cityRepository, CityFactory cityFactory) {
        this.cityRepository = cityRepository;
        this.cityFactory = cityFactory;
    }

    public List<CityResponseDto> findAllCity() {
        List<City> cityList = cityRepository.findAll();
        if(cityList.isEmpty()) {
            return new ArrayList<>();
        } else {
            return cityList.stream()
                    .map(cityFactory::makeCityDto)
                    .toList();
        }
    }
}
