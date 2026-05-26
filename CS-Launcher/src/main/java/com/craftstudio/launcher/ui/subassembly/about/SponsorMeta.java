package com.craftstudio.launcher.ui.subassembly.about;

public class SponsorMeta {
    public Sponsor[] sponsors;

    public static class Sponsor {
        private final String name;
        private final String time;
        private final String identifier;
        private final String avatar;
        private final float amount;

        public Sponsor(String name, String time, String identifier, String avatar, float amount) {
            this.name = name;
            this.time = time;
            this.identifier = identifier;
            this.avatar = avatar;
            this.amount = amount;
        }

        public String getName() {
            return name;
        }

        public String getTime() {
            return time;
        }

        public String getIdentifier() {
            return identifier;
        }

        public String getAvatar() {
            return avatar;
        }

        public float getAmount() {
            return amount;
        }
    }
}
