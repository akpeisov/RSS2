package kz.home.RelaySmartSystems.filters;

public class PositiveIntegerFilter {
    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return true;
        }
        int value = (Integer)other;
        return value <= 0;
    }
}
