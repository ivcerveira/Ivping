package com.nuapps.ivping.util;

public final class Ipv4SubnetCalculator {
    private static final long IPV4_BITS = 0xffff_ffffL;

    private Ipv4SubnetCalculator() {
    }

    public record Result(String networkAddress, String usableHostRange, String broadcastAddress,
                         long totalHosts, long usableHosts, String subnetMask, String cidrNotation) {
    }

    public static Result calculate(String address, int prefix) {
        if (prefix < 1 || prefix > 32) {
            throw new IllegalArgumentException("Selecione uma máscara de sub-rede válida.");
        }
        String input = address == null ? "" : address.trim();
        String[] octets = input.split("\\.", -1);
        if (octets.length != 4) {
            throw invalidAddress();
        }
        long ip = 0;
        for (String octet : octets) {
            if (!octet.matches("[0-9]{1,3}")) {
                throw invalidAddress();
            }
            int value = Integer.parseInt(octet);
            if (value > 255) {
                throw invalidAddress();
            }
            ip = (ip << 8) | value;
        }

        long mask = (IPV4_BITS << (32 - prefix)) & IPV4_BITS;
        long network = ip & mask;
        long lastAddress = network | (IPV4_BITS ^ mask);
        long total = 1L << (32 - prefix);
        // /31 uses both addresses on point-to-point links; /32 identifies one host.
        long usable = prefix >= 31 ? total : total - 2;
        long firstHost = prefix >= 31 ? network : network + 1;
        long lastHost = prefix >= 31 ? lastAddress : lastAddress - 1;
        String range = prefix == 32 ? format(firstHost) : format(firstHost) + " - " + format(lastHost);
        return new Result(format(network), range,
                prefix >= 31 ? "Não se aplica" : format(lastAddress),
                total, usable, format(mask), "/" + prefix);
    }

    private static IllegalArgumentException invalidAddress() {
        return new IllegalArgumentException("Digite um endereço IPv4 válido. Exemplo: 192.168.0.1");
    }

    private static String format(long address) {
        return ((address >>> 24) & 255) + "." + ((address >>> 16) & 255) + "."
                + ((address >>> 8) & 255) + "." + (address & 255);
    }
}
