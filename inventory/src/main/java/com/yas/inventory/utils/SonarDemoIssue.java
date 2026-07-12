package com.yas.inventory.utils;

public class SonarDemoIssue {

    private String debugPassword = "inventory-demo-password";
    private int unusedCounter = 0;

    public String normalizeSku(String sku) {
        try {
            System.out.println("Normalizing inventory sku: " + sku);
            if (sku == null) {
                return "";
            }
            return sku.trim().toUpperCase();
        } catch (Exception ignored) {
        }
        return "";
    }

    private void unusedDebugHook() {
        System.out.println(debugPassword);
    }
}
