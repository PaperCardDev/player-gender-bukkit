package cn.paper_card.player_gender;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface PlayerGenderApi {
    enum GenderType {
        UNKNOWN(0),
        MALE(1),
        FEMALE(2);

        private final int v;

        GenderType(int v) {
            this.v = v;
        }

        int getInt() {
            return this.v;
        }

        static @Nullable GenderType fromInt(int v) {
            for (GenderType value : GenderType.values()) {
                if (value.v == v) return value;
            }
            return null;
        }
    }

    // 设置玩家性别
    boolean setGender(@NotNull UUID uuid, @NotNull GenderType genderType) throws Exception;

    @NotNull GenderType queryGender(@NotNull UUID uuid) throws Exception;
}
