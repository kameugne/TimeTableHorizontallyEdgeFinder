package org.example;

import java.io.File;
import java.util.Scanner;

public class RCPSPInstance {
    public int[] capacities;
    public int numberOfTasks;
    public int numberOfResources;
    public int heights[][];
    public int[] processingTimes;
    public int[][] precedences;
    int processingTimesSum;

    public RCPSPInstance(String fileName) throws Exception {
        Scanner s = new Scanner (new File(fileName)).useDelimiter("\\s+");
        while (!s.hasNextInt() )
            s.nextLine();

        numberOfTasks = s.nextInt();
        numberOfResources = s.nextInt();
        capacities = new int[numberOfResources];
        for(int i=0; i<numberOfResources; i++)
        {
            capacities[i] = s.nextInt();
        }

        precedences = new int[numberOfTasks][numberOfTasks];

        for(int i=0; i<numberOfTasks; i++)
        {
            for(int j=0; j<numberOfTasks; j++)
            {
                precedences[i][j] = -1;
            }
        }
        heights = new int[numberOfResources][numberOfTasks];
        processingTimes = new int[numberOfTasks];
        processingTimesSum = 0;

        int nbSuccesseurs;
        for(int i=0; i<numberOfTasks; i++)
        {
            processingTimes[i] = s.nextInt();
            processingTimesSum += processingTimes[i];

            for(int j=0; j<numberOfResources; j++)
            {
                //Height[j][i] = x => la tâche i utilise x unités sur la ressource j
                heights[j][i] = s.nextInt();
            }
            int k;
            nbSuccesseurs = s.nextInt();
            for(int j=0; j<nbSuccesseurs; j++)
            {
                //i < k
                //Le fichier est en base 1
                k = s.nextInt();

                if(precedences[i][k-1] == 0 || precedences[k-1][i] == 1)
                    throw new Exception("Les précédences du problème sont incohérentes.");

                precedences[i][k-1] = 1;
                precedences[k-1][i] = 0;
            }
        }
        s.close();
    }

    public int horizon()
    {
        return processingTimesSum;
    }

}
