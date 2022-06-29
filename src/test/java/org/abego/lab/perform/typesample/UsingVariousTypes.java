package org.abego.lab.perform.typesample;

public class UsingVariousTypes {
    public static class Foo {
        private final String text;
        private final int value;

        public Foo(String text, int value) {
            this.text = text;
            this.value = value;
        }

        @Override
        public String toString() {
            return "Foo{" +
                    "text='" + text + '\'' +
                    ", value=" + value +
                    '}';
        }
    }

    public String byteToString(byte value) {
        return "byte: "+ Byte.toString(value);
    }

    public String shortToString(short value) {
        return "short: "+ Short.toString(value);
    }

    public String intToString(int value) {
        return "int: "+ Integer.toString(value);
    }

    public String longToString(long value) {
        return "long: "+ Long.toString(value);
    }

    public String floatToString(float value) {
        return "float: "+ Float.toString(value);
    }

    public String doubleToString(double value) {
        return "double: "+ Double.toString(value);
    }

    public String charToString(char value) {
        return "char: "+ Character.toString(value);
    }

    public String booleanToString(boolean value) {
        return "boolean: "+ Boolean.toString(value);
    }

    public String longArrayToString(long[] value) {
        StringBuilder sb = new StringBuilder();
        for (long i : value) {
            sb.append(i);
            sb.append('\n');
        }
        return sb.toString();
    }

    public String longArrayArrayToString(long[][] value) {
        StringBuilder sb = new StringBuilder();
        for (long[] i : value) {
            for (long j : i) {
                sb.append(j);
                sb.append(' ');
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    public String stringArrayToText(String[] strings) {
        StringBuilder sb = new StringBuilder();
        for (String i : strings) {
            sb.append(i);
            sb.append('\n');
        }
        return sb.toString();
    }

    public String fooArrayToString(Foo[] value) {
        StringBuilder sb = new StringBuilder();
        for (Foo i : value) {
            sb.append(i);
            sb.append('\n');
        }
        return sb.toString();
    }

}
