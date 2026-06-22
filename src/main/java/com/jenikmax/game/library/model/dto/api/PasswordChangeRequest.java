package com.jenikmax.game.library.model.dto.api;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class PasswordChangeRequest {

    @NotBlank
    @Size(min = 4, max = 100)
    private String currentPassword;

    @NotBlank
    @Size(min = 4, max = 100)
    private String newPassword;

    public String getCurrentPassword() { return currentPassword; }
    public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
