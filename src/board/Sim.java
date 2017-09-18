package board;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Stack;

public class Sim {
    static final int R = 5; // how many rows
    static final int C = 5;
    static final int N = R * C;
    static byte[] board = new byte[N];
    
    static final int TPA = 6;
    static final int MATCH = 5;
    static final int COMBO = 3;
    
    public static void main(String[] args) {
        
        byte[][] optimal = new byte[R * C + 1][R * C];
        int[] total = new int[R * C + 1];
        int percent = 0;
        int Big = 1 << R * C;
        int Small = 100;
        int howmany = Big;
        
        for (int i = 0; i < howmany; i++) {
            if (i % ((howmany) / 100) == 0) {
                System.out.println(++percent + "% done");
            }
            
            fill(board, i);
            int score = score(board);
            int bo = count(board);
            if (total[bo] < score) {
                total[bo] = score;
                optimal[bo] = Arrays.copyOf(board, board.length);
            }
        }
        // int x = 0b1100010100010100101100100;
        // fill(board, x);
        // // print(board);
        // int score = score(board);
        // System.out.println(score);
        for (int i = 0; i < total.length; i++) {
            // System.out.println(total[i]);
            print(optimal[i]);
            System.out.println();
        }
    }
    
    static void print(byte[] board) {
        int i = 0;
        for (int r = 0; r < R; r++) {
            for (int c = 0; c < C; c++) {
                System.out.print(board[i] == -1 ? " " : board[i]);
                i++;
            }
            System.out.print("H");
        }
        System.out.println();
    }
    
    static void fill(byte[] board, int val) {
        for (int i = 0; i < N; i++) {
            board[i] = (byte) ((val & (1 << i)) >> i);
            
        }
    }
    
    static int count(byte[] board) {
        int t = 0;
        for (byte b : board)
            if (b == 1)
                t++;
        return t;
    }
    
    static int score(byte[] board) {
        byte[] b = new byte[N];
        for (int i = 0; i < N; i++) {
            b[i] = board[i];
        }
        int score = 0;
        byte[] match = match(b);
        int ret = mergeCombo(b, match);
        score += ret;
        // System.out.println("Get: " + ret);
        fall(b, match);
        // print(b);
        while (ret != 0) {
            match = match(b);
            ret = mergeCombo(b, match);
            // System.out.println("Get: " + ret);
            score += ret;
            fall(b, match);
            // print(b);
        }
        return score;
    }
    static final byte DNE = -1;
    static final byte UNK = -2;
    static final byte CLR = -3;
    static final byte MAT = -4;
    
    static byte[] match(byte[] b) {
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
    
    static int mergeCombo(byte[] b, byte[] match) {
        int score = 0;
        
        byte OnColor = 1;
        for (int i = 0; i < N; i++) {
            if (match[i] != MAT) {
                if (match[i] == CLR) {
                    boolean[] vis = new boolean[N];
                    Stack<Integer> s = new Stack<>();
                    s.push(i);
                    byte type = b[i];
                    int count = 0;
                    while (!s.isEmpty()) {
                        int x = s.pop();
                        if (!vis[x]) {
                            // System.out.println("visit: " + x);
                            vis[x] = true;
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
                    score +=
                            (type == OnColor ? count == 4 ? TPA : MATCH : COMBO);
                    // System.out.println("score: " + score);
                }
            }
            
        }
        return score;
    }
    
    static void fall(byte[] b, byte[] match) {
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
            // System.out.println("C"
            // + c + ", left: " + left + ", matched: " + matched);
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
    }
    
    static Iterator<Integer> surround(int pos) {
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
    
    static int left(int pos) {
        if (pos % C == 0)
            return -1;
        return pos - 1;
    }
    
    static int right(int pos) {
        if (pos % C == C - 1)
            return -1;
        return pos + 1;
    }
    
    static int up(int pos) {
        if (pos < C)
            return -1;
        return pos - C;
    }
    
    static int down(int pos) {
        if (pos >= R * C - C)
            return -1;
        return pos + C;
    }
}
