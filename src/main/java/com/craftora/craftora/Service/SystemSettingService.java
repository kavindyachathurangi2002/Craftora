package com.craftora.craftora.Service;

import com.craftora.craftora.Entity.SystemSetting;
import com.craftora.craftora.Repository.SystemSettingRepository;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class SystemSettingService {

    private final SystemSettingRepository systemSettingRepository;

    public SystemSettingService(
            SystemSettingRepository systemSettingRepository) {

        this.systemSettingRepository =
                systemSettingRepository;
    }


    // =====================================================
    // GET SETTING VALUE
    // =====================================================

    public String getSetting(String settingKey) {

        return systemSettingRepository
                .findBySettingKey(settingKey)
                .map(SystemSetting::getSettingValue)
                .orElse(null);
    }


    // =====================================================
    // GET ARTISAN REGISTRATION FEE
    // =====================================================

    public BigDecimal getArtisanRegistrationFee() {

        String value =
                getSetting("ARTISAN_REGISTRATION_FEE");

        if (value == null || value.isBlank()) {

            return new BigDecimal("2500.00");
        }

        return new BigDecimal(value);
    }


    // =====================================================
    // GET PLATFORM COMMISSION
    // =====================================================

    public BigDecimal getPlatformCommission() {

        String value =
                getSetting("PLATFORM_COMMISSION");

        if (value == null || value.isBlank()) {

            return new BigDecimal("10.00");
        }

        return new BigDecimal(value);
    }


    // =====================================================
    // GET EMAIL NOTIFICATION STATUS
    // =====================================================

    public boolean isEmailNotificationsEnabled() {

        String value =
                getSetting("EMAIL_NOTIFICATIONS");

        return value != null
                && value.equalsIgnoreCase("ON");
    }


    // =====================================================
    // GET MINIMUM PASSWORD LENGTH
    // =====================================================

    public int getMinimumPasswordLength() {

        String value =
                getSetting("MINIMUM_PASSWORD_LENGTH");

        if (value == null || value.isBlank()) {

            return 8;
        }

        return Integer.parseInt(value);
    }


    // =====================================================
    // SAVE / UPDATE SETTING
    // =====================================================

    public void updateSetting(
            String settingKey,
            String settingValue) {

        SystemSetting setting =
                systemSettingRepository
                        .findBySettingKey(settingKey)
                        .orElseGet(() -> {

                            SystemSetting newSetting =
                                    new SystemSetting();

                            newSetting.setSettingKey(
                                    settingKey
                            );

                            return newSetting;
                        });


        setting.setSettingValue(
                settingValue
        );


        systemSettingRepository.save(
                setting
        );
    }
}