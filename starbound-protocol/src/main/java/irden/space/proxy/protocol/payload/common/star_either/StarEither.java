package irden.space.proxy.protocol.payload.common.star_either;

public record StarEither<A, B>(A left, B right) {
    public static <A, B> StarEither<A, B> left(A value) {
        return new StarEither<>(value, null);
    }

    public static <A, B> StarEither<A, B> right(B value) {
        return new StarEither<>(null, value);
    }

    public boolean isLeft() {
        return left != null;
    }

    public boolean isRight() {
        return right != null;
    }

    @Override
    public A left() {
        if (left == null) {
            throw new IllegalStateException("No left value present");
        }
        return left;
    }

    @Override
    public B right() {
        if (right == null) {
            throw new IllegalStateException("No right value present");
        }
        return right;
    }

    @Override
    public String toString() {
        if (isLeft()) {
            return "Left(" + left + ")";
        } else {
            return "Right(" + right + ")";
        }
    }

}
