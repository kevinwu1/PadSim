package board;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.function.Function;
import java.util.stream.IntStream;

public class Simulator {
    final int R, C, N;
    
    Function<List<Match>, Double> score;
    
    Simulator(int W, int H, Function<List<Match>, Double> score) {
        C = W;
        R = H;
        N = R * C;
        this.score = score;
    }
    
    SimResult run() {
        int[] optimal = new int[R * C + 1];
        double[] total = new double[R * C + 1];
        int howmany = 1 << R * C;
        System.out.println(howmany);
        
        Runnable count = new Runnable() {
            int total = 0;
            
            @Override
            public void run() {
                synchronized (this) {
                    total++;
                    if (total % (howmany / 100) == 0) {
                        System.out.println(total / (howmany / 100) + "% Done");
                    }
                }
            }
        };
        
        IntStream.range(0, howmany).parallel().forEach(i -> {
            byte[] board = fill(i);
            double score = score(board);
            int bo = Integer.bitCount(i);
            synchronized (this) {
                if (total[bo] < score) {
                    total[bo] = score;
                    optimal[bo] = i;
                }
            }
            count.run();
        });
        return new SimResult(R, C, optimal);
    }
    
    class SimResult {
        int R, C, N;
        int[] optimal;
        
        private SimResult(int R, int C, int[] optimal) {
            this.R = R;
            this.C = C;
            this.N = R * C;
            this.optimal = optimal;
        }
        
        void printBoards() {
            Arrays.stream(optimal).forEach(
                    val -> {
                        int i = 0;
                        for (int r = 0; r < R; r++) {
                            for (int c = 0; c < C; c++) {
                                System.out.print(((val & (1 << i)) >> i) == -1
                                        ? " " : ((val & (1 << i)) >> i));
                                i++;
                            }
                            // System.out.print("H");
                        }
                        System.out.println();
                    });
        }
        
        void printCodes() {
            Arrays.stream(optimal).forEach(
                    b -> System.out.println(new StringBuilder(pad(N, Integer
                            .toBinaryString(b))).reverse().toString()));
        }
        
        int[] getOptimal() {
            return optimal;
        }
        
        private String pad(int howmany, String s) {
            return new String(new char[howmany - s.length()])
                    .replace('\0', '0')
                    + s;
            
        }
    }
    
    byte[] fill(int val) {
        byte[] board = new byte[N];
        for (int i = 0; i < N; i++) {
            board[i] = (byte) ((val & (1 << i)) >> i);
        }
        return board;
    }
    
    double score(byte[] board) {
        byte[] b = new byte[N];
        for (int i = 0; i < N; i++) {
            b[i] = board[i];
        }
        List<Match> matches = new ArrayList<>();
        boolean cont = false;
        do {
            byte[] match = match(b);
            mergeCombo(b, match, matches);
            cont = fall(b, match);
        } while (cont);
        return score.apply(matches);// score * (1.0 + (combos[0] - 1) * .25);
    }
    static final byte DNE = -1;
    static final byte UNK = -2;
    static final byte CLR = -3;
    static final byte MAT = -4;
    
    byte[] match(byte[] b) {
        byte[] match = new byte[N];
        Arrays.fill(match, UNK);
        for (int i = 0; i < N; i++) {
            if (b[i] == DNE) {
                match[i] = DNE;
            }
            else {
                int r = right(i);
                int r2 = right(r);
                int d = down(i);
                int d2 = down(d);
                if (r != -1 && r2 != -1) {
                    if (b[i] == b[r] && b[r] == b[r2]) {
                        match[i] = match[r] = match[r2] = CLR;
                        int r3 = right(r2);
                        while (r3 != -1 && b[r3] == b[r2]) {
                            match[r3] = CLR;
                            r2 = r3;
                            r3 = right(r2);
                        }
                    }
                }
                if (d != -1 && d2 != -1) {
                    if (b[i] == b[d] && b[d] == b[d2]) {
                        match[i] = match[d] = match[d2] = CLR;
                        int d3 = down(d2);
                        while (d3 != -1 && b[d3] == b[d2]) {
                            match[d3] = CLR;
                            d2 = d3;
                            d3 = down(d2);
                        }
                    }
                }
            }
        }
        return match;
    }
    
    void mergeCombo(byte[] b, byte[] match, List<Match> m) {
        byte OnColor = 1;
        for (int i = 0; i < N; i++) {
            if (match[i] != MAT) {
                if (match[i] == CLR) {
                    boolean[] vis = new boolean[N];
                    Stack<Integer> s = new Stack<>();
                    s.push(i);
                    byte type = b[i];
                    int count = 0;
                    List<Integer> orbs = new ArrayList<>();
                    while (!s.isEmpty()) {
                        int x = s.pop();
                        if (!vis[x]) {
                            // System.out.println("visit: " + x);
                            vis[x] = true;
                            orbs.add(x);
                            count++;
                            match[x] = MAT;
                            Iterator<Integer> it = surround(x);
                            while (it.hasNext()) {
                                int n = it.next();
                                if (!vis[n] && b[n] == type && match[n] == CLR) {
                                    s.push(n);
                                }
                            }
                        }
                    }
                    if (count >= 3) {
                        m.add(new Match(orbs, type == OnColor));
                    }
                }
            }
            
        }
    }
    
    boolean fall(byte[] b, byte[] match) {
        boolean cont = false;
        for (int c = 0; c < C; c++) {
            int left = 0;
            int matched = 0;
            for (int r = 0; r < R; r++) {
                if (match[c + r * C] != DNE) {
                    if (match[c + r * C] == MAT) {
                        matched++;
                    }
                    else {
                        left++;
                    }
                }
            }
            if (matched != 0) {
                cont = true;
            }
            for (int i = 0; i < left; i++) {
                int pos = c + R * C - C - i * C;
                int where = pos;
                while (match[where] == MAT)
                    where -= C;
                b[pos] = b[where];
                if (where != pos)
                    b[where] = DNE;
                match[where] = MAT;
            }
            for (int i = 0; i < matched; i++) {
                int pos = c + R * C - C - left * C - i * C;
                b[pos] = DNE;
            }
        }
        return cont;
    }
    
    class Match {
        boolean onColor;
        int count;
        boolean row;
        boolean col;
        boolean cross;
        boolean box;
        
        private Match(List<Integer> orbs, boolean onColor) {
            this.onColor = onColor;
            this.count = orbs.size();
            if (count == R) {
                boolean fuaTop = false;
                boolean fuaBot = false;
                for (Integer x : orbs) {
                    fuaTop |= up(x) == -1;
                    fuaBot |= down(x) == -1;
                }
                col = fuaTop && fuaBot;
            }
            
            // cross
            if (count == 5) {
                int min = orbs.stream().reduce((x, y) -> x < y ? x : y).get();
                int max = orbs.stream().reduce((x, y) -> x < y ? y : x).get();
                // System.out.println("min, max: " + min + ", " + max);
                cross =
                        min % C == max % C
                                && min + 2 * C == max
                                && orbs.contains(min + C - 1)
                                && orbs.contains(min + C + 1);
            }
            // row
            if (count >= C) {
                for (int r = 0; r < R; r++) {
                    boolean isRow = true;
                    for (int c = 0; c < C; c++) {
                        if (!orbs.contains(r * C + c)) {
                            isRow = false;
                            break;
                        }
                    }
                    if (isRow) {
                        row = true;
                        break;
                    }
                }
            }
            
            // box
            if (count == 9) {
                int min = orbs.stream().reduce((x, y) -> x < y ? x : y).get();
                boolean isBox = true;
                for (int r = 0; r < 3; r++) {
                    for (int c = 0; c < 3; c++) {
                        if (!orbs.contains(r * C + c + min)) {
                            isBox = false;
                        }
                    }
                }
                box = isBox;
                
            }
        }
        
    }
    
    Iterator<Integer> surround(int pos) {
        Iterator<Integer> it = new Iterator<Integer>() {
            int[] vals = { left(pos), right(pos), up(pos), down(pos) };
            int ind = 0;
            
            @Override
            public boolean hasNext() {
                if (ind >= 4)
                    return false;
                if (vals[ind] != -1) {
                    return true;
                }
                ind++;
                return hasNext();
            }
            
            @Override
            public Integer next() {
                if (ind >= 4) {
                    return -1;
                }
                if (vals[ind] != -1) {
                    return vals[ind++];
                }
                ind++;
                return next();
            }
            
        };
        return it;
    }
    
    int left(int pos) {
        if (pos % C == 0)
            return -1;
        return pos - 1;
    }
    
    int right(int pos) {
        if (pos % C == C - 1)
            return -1;
        return pos + 1;
    }
    
    int up(int pos) {
        if (pos < C)
            return -1;
        return pos - C;
    }
    
    int down(int pos) {
        if (pos >= R * C - C)
            return -1;
        return pos + C;
    }
    
    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        new Simulator(6, 5, new Function<List<Match>, Double>() {
            @Override
            public Double apply(List<Match> t) {
                double score = 0;
                int crosses = 0;
                for (Match m : t) {
                    if (m.onColor) {
                        score += 1 + 0.25 * (m.count - 3);
                    }
                    if (m.cross) {
                        crosses++;
                    }
                }
                return score * (1 + t.size() * 0.25) * (crosses * 9);
            }
        }).run().printCodes();
        long end = System.currentTimeMillis();
        System.out.println("Took: " + (end - start) / 1000);
    }
}
