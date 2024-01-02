package org.example;

public class Main {
    /*public static void main(String[] args) throws Exception {
        for (int i = 9; i <= 9; i++) {
            for (int k = 0; k < 7; k++) {
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