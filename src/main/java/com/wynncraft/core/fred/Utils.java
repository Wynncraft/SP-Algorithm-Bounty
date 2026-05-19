package com.wynncraft.core.fred;

public final class Utils {
    public static final class Vec5 {
        public int a, b, c, d, e;

        public Vec5(int[] x) {
            if (x.length != 5)
                throw new IllegalArgumentException("Input must have length 5.");
            a = x[0];
            b = x[1];
            c = x[2];
            d = x[3];
            e = x[4];
        }

        public Vec5(Vec5 x) {
            a = x.a;
            b = x.b;
            c = x.c;
            d = x.d;
            e = x.e;
        }

        public Vec5(int a, int b, int c, int d, int e) {
            this.a = a;
            this.b = b;
            this.c = c;
            this.d = d;
            this.e = e;
        }

        public Vec5() {
        }

        public static boolean meetsReqs(Vec5 sp, Vec5 reqs) {
            return (reqs.a == 0 || reqs.a <= sp.a)
                    && (reqs.b == 0 || reqs.b <= sp.b)
                    && (reqs.c == 0 || reqs.c <= sp.c)
                    && (reqs.d == 0 || reqs.d <= sp.d)
                    && (reqs.e == 0 || reqs.e <= sp.e);
        }

        public void add(Vec5 y) {
            a += y.a;
            b += y.b;
            c += y.c;
            d += y.d;
            e += y.e;
        }

        public void addNegatives(Vec5 y) {
            if (y.a < 0)
                a += y.a;
            if (y.b < 0)
                b += y.b;
            if (y.c < 0)
                c += y.c;
            if (y.d < 0)
                d += y.d;
            if (y.e < 0)
                e += y.e;
        }

        public void sub(Vec5 y) {
            a -= y.a;
            b -= y.b;
            c -= y.c;
            d -= y.d;
            e -= y.e;
        }

        public void setFrom(Vec5 y) {
            a = y.a;
            b = y.b;
            c = y.c;
            d = y.d;
            e = y.e;
        }

        public void setFrom(int[] y) {
            if (y.length != 5)
                throw new IllegalArgumentException("Input must have length 5.");
            a = y[0];
            b = y[1];
            c = y[2];
            d = y[3];
            e = y[4];
        }

        public int total() {
            return a + b + c + d + e;
        }

        public boolean hasNeg() {
            return a < 0 || b < 0 || c < 0 || d < 0 || e < 0;
        }

        public boolean isZero() {
            return a == 0 && b == 0 && c == 0 && d == 0 && e == 0;
        }

        @Override
        public String toString() {
            return "(" + a + ", " + b + ", " + c + ", " + d + ", " + e + ")";
        }

        public int[] asArray() {
            return new int[]{a, b, c, d, e};
        }
    }

    public static final class IntStack {
        private int[] stack;
        private int top = 0;

        public IntStack(int init_capacity) {
            stack = new int[init_capacity];
        }

        public void push(int x) {
            if (top == stack.length)
                expand();
            stack[top++] = x;
        }

        public int pop() {
            return stack[--top];
        }

        public boolean isEmpty() {
            return top == 0;
        }

        private void expand() {
            int newCap = stack.length << 1;
            int[] newStack = new int[newCap];
            System.arraycopy(stack, 0, newStack, 0, stack.length);
            stack = newStack;
        }

        public void clear() {
            top = 0;
        }
    }
}
