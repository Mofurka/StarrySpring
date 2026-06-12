package irden.space.proxy.protocol.codec.variant;

import java.util.Arrays;

public record ListVariantValue(VariantValue[] values) implements VariantValue {
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		return obj instanceof ListVariantValue other && Arrays.equals(values, other.values);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(values);
	}

	@Override
	public String toString() {
		return "ListVariantValue[values=" + Arrays.toString(values) + ']';
	}
}
