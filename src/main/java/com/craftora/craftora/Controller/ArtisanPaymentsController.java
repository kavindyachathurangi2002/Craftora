package com.craftora.craftora.Controller;

import com.craftora.craftora.Entity.Artisan;
import com.craftora.craftora.Entity.ArtisanOrder;
import com.craftora.craftora.Repository.ArtisanOrderRepository;
import com.craftora.craftora.Repository.ArtisanRepository;
import com.craftora.craftora.Service.SystemSettingService;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.util.List;

@Controller
public class ArtisanPaymentsController {

    private final ArtisanRepository artisanRepository;

    private final ArtisanOrderRepository artisanOrderRepository;

    private final SystemSettingService systemSettingService;


    public ArtisanPaymentsController(

            ArtisanRepository artisanRepository,

            ArtisanOrderRepository artisanOrderRepository,

            SystemSettingService systemSettingService) {

        this.artisanRepository = artisanRepository;

        this.artisanOrderRepository = artisanOrderRepository;

        this.systemSettingService = systemSettingService;
    }


    @GetMapping("/artisan/payments")

    public String artisanPayments(

            HttpSession session,

            Model model) {


        Object loggedInUserId =
                session.getAttribute("loggedInUserId");


        if (loggedInUserId == null) {

            return "redirect:/login";
        }


        Long userId =
                Long.valueOf(
                        loggedInUserId.toString()
                );


        Object selectedRole =
                session.getAttribute("selectedRole");


        if (!(selectedRole instanceof String)

                || !"ARTISAN".equalsIgnoreCase(
                        selectedRole.toString()
                )) {

            return "redirect:/login";
        }


        Artisan artisan =
                artisanRepository

                        .findByUserUserId(userId)

                        .orElse(null);


        if (artisan == null) {

            return "redirect:/artisan-dashboard";
        }


        List<ArtisanOrder> artisanOrders =

                artisanOrderRepository

                        .findByArtisanOrderByCreatedAtDesc(
                                artisan
                        );


        BigDecimal totalSales =
                BigDecimal.ZERO;


        BigDecimal totalCommission =
                BigDecimal.ZERO;


        BigDecimal totalRevenue =
                BigDecimal.ZERO;


        BigDecimal platformCommission =

                systemSettingService
                        .getPlatformCommission();


        for (ArtisanOrder artisanOrder :
                artisanOrders) {


            if (artisanOrder.getSubtotal() != null) {

                totalSales =
                        totalSales.add(
                                artisanOrder.getSubtotal()
                        );
            }


            if (artisanOrder.getCommissionAmount() != null) {

                totalCommission =

                        totalCommission.add(
                                artisanOrder
                                        .getCommissionAmount()
                        );
            }


            if (artisanOrder.getArtisanAmount() != null) {

                totalRevenue =

                        totalRevenue.add(
                                artisanOrder
                                        .getArtisanAmount()
                        );
            }
        }


        model.addAttribute(
                "artisan",
                artisan
        );


        model.addAttribute(
                "artisanOrders",
                artisanOrders
        );


        model.addAttribute(
                "totalSales",
                totalSales
        );


        model.addAttribute(
                "totalCommission",
                totalCommission
        );


        model.addAttribute(
                "totalRevenue",
                totalRevenue
        );


        model.addAttribute(
                "platformCommission",
                platformCommission
        );


        return "artisan-payments";
    }
}