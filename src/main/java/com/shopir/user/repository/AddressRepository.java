package com.shopir.user.repository;

import com.shopir.user.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AddressRepository extends JpaRepository<Address, Long> {
    Address findByWebUser_IdWebUser(Long idWebUser);
}
