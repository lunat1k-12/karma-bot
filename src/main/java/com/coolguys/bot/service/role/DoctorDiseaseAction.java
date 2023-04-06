package com.coolguys.bot.service.role;

import com.coolguys.bot.dto.ChatAccount;
import com.coolguys.bot.service.DoctorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static com.coolguys.bot.service.role.RoleActionType.DOCTOR_MAKE_SICK;

@Component
@RequiredArgsConstructor
public class DoctorDiseaseAction implements RoleAction {

    private final DoctorService doctorService;

    @Override
    public void doAction(ChatAccount acc) {
        doctorService.processPatientSelection(acc);
    }

    @Override
    public String getActionType() {
        return DOCTOR_MAKE_SICK;
    }
}
