package com.bitsyko.libicons.shader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DataHolder {

    public int A;
    public int R;
    public int G;
    public int B;
    private List<Exec> execList = new ArrayList<>();


    public DataHolder(int a, int r, int g, int b) {
        A = a;
        R = r;
        G = g;
        B = b;
    }

    public DataHolder() {

    }

    public void addExec(Exec... execs) {
        execList.addAll(Arrays.asList(execs));
    }


}
