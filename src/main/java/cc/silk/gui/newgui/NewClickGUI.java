private void initPanels() {
        try {
            int x = 20;
            int y = 20;

            for (Category category : Category.values()) {
                if (category == Category.CONFIG)
                    continue;

                List<Module> modules = cc.silk.SilkClient.INSTANCE.getModuleManager().getModulesByCategory(category);
                if (!modules.isEmpty()) {
                    panels.add(new CategoryPanel(category, x, y, PANEL_WIDTH));
                    x += PANEL_WIDTH + PANEL_SPACING;
                }
            }

            configPanel = new ConfigPanel(x, y, PANEL_WIDTH);
        } catch (Exception ignored) {}
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        // Har panel aur config panel ko Minecraft ke DrawContext se render karein taaki screen par sab dikhe
        int centerX = width / 2;
        int centerY = height / 2;
        float alpha = 1.0f;
        float scale = 1.0f;

        try {
            for (CategoryPanel panel : panels) {
                panel.update(delta);
                panel.render(mouseX, mouseY, alpha, scale, centerX, centerY);
            }

            if (configPanel != null) {
                configPanel.update(delta);
                configPanel.render(mouseX, mouseY, alpha, scale, centerX, centerY);
            }
        } catch (Exception e) {
            // Agar koi error aaye toh ignore ho jaye aur game crash na ho
        }
    }
