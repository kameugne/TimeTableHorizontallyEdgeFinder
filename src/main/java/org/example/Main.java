package org.example;

public class Main {
    /*public static void main(String[] args) throws Exception {
        for (int i = 1; i <= 20; i++) {
            for (int k = 6; k < 8; k++) {
                RunRCPSP sample = new RunRCPSP("Data/BL/bl20_" + i + ".rcp", k, 0, 100);
                System.out.println(sample.AllResults());
            }
        }
    }*/
    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            throw new IllegalStateException("Please enter correct parameters.");
        }
        final String filename = args[0];
        final int prop = Integer.parseInt(args[1]);
        final int search = Integer.parseInt(args[2]);
        final int timelimite = Integer.parseInt(args[3]);
        RunRCPSP sample = new RunRCPSP(filename, prop, search, timelimite);
        System.out.println(sample.AllResults());

    }
}