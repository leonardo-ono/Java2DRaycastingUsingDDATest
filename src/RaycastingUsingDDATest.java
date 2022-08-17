import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * 2D Raycasting using Digital differential analyzer (DDA) / Test #1
 * 
 * @author Leonardo Ono (ono.leo80@gmail.com)
 */
public class RaycastingUsingDDATest extends JPanel 
                            implements KeyListener, MouseMotionListener {

    static final int TILE_SIZE = 50;
    
    private final int[][] map = {
        {1,1,1,1,1,1,1,1,1,1},
        {1,0,0,0,0,1,0,0,0,1},
        {1,0,1,0,0,1,0,1,1,1},
        {1,0,0,0,0,0,0,0,0,1},
        {1,0,0,0,1,1,1,0,0,0},
        {1,0,0,0,0,0,0,0,0,0},
        {1,0,0,0,0,0,0,0,0,1},
        {1,0,0,1,1,0,0,0,0,1},
        {1,0,0,0,0,0,0,0,0,1},
        {1,1,1,1,1,1,1,1,1,1},
    };
    
    private final int mapRows = map.length;
    private final int mapCols = map[0].length;
    
    private final Point.Double player = new Point.Double(3.25, 4.75);
    private final Point.Double mouse = new Point.Double();
    
    public RaycastingUsingDDATest() {
    }

    public void start() {
        setPreferredSize(new Dimension(800, 600));
        addKeyListener(this);
        addMouseMotionListener(this);
    }
    
    private static final int MAX_RAY_SIZE = 1000;
            
    private final Point rayCell = new Point();
    
    // ray-wall intersection point result
    private final Point.Double ip = new Point.Double();
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        update();
        Graphics2D g2d = (Graphics2D) g;
        drawMap(g2d);
        
        drawPlayerMouseRay(g2d);
        drawPlayer(g2d);
        
        int side = performDDA(player, mouse, rayCell, ip, MAX_RAY_SIZE);
        
        if (side > 0) {
            // draw intersected wall
            switch (side) {
                case 1 -> g.setColor(Color.YELLOW);
                case 2 -> g.setColor(Color.ORANGE);
            }
            g.fillRect(rayCell.x * TILE_SIZE, rayCell.y * TILE_SIZE
                                                    , TILE_SIZE, TILE_SIZE);

            // draw ray-wall intersection point
            g.setColor(Color.RED);
            g.fillOval((int) (ip.x * TILE_SIZE - 3)
                                , (int) (ip.y * TILE_SIZE - 3), 6, 6);
        }
        

        try {
            Thread.sleep(1000 / 60);
        } catch (InterruptedException ex) { }
        repaint();
    }
    
    private static final double DIV_BY_ZERO_REPLACE = 0.000000001;
    
    // refs.: javidx9 - https://www.youtube.com/watch?v=NbSee-XM7WA&t=815s
    //        https://lodev.org/cgtutor/raycasting.html
    //
    // return: 0 - intersection not detected
    //         1 - intersection horizontal wall
    //         2 - intersection vertical wall
    private int performDDA(Point.Double src, Point.Double dst
                            ,Point rayCell, Point.Double intersectionPoint
                                                    , double maxRayDistance) {

        double dy = dst.y - src.y;
        double dx = dst.x - src.x;
        dx = dx == 0 ? DIV_BY_ZERO_REPLACE : dx;
        dy = dy == 0 ? DIV_BY_ZERO_REPLACE : dy;
        double distInv = 1.0 / Math.hypot(dx, dy);
        dx *= distInv;
        dy *= distInv;
        int dxSign = (int) Math.signum(dx);
        int dySign = (int) Math.signum(dy);
        rayCell.setLocation((int) src.x, (int) src.y);
        double startDy = rayCell.y + dySign * 0.5 + 0.5 - src.y;
        double startDx = rayCell.x + dxSign * 0.5 + 0.5 - src.x;
        double distDx = Math.abs(1 / dx);
        double distDy = Math.abs(1 / dy);
        double totalDistDx = distDx * dxSign * startDx;
        double totalDistDy = distDy * dySign * startDy;
        double intersectionDistance = 0;
        int side = 0;
        while (intersectionDistance < maxRayDistance) {
            if (totalDistDx < totalDistDy) {
                rayCell.x += dxSign;
                intersectionDistance = totalDistDx;
                totalDistDx += distDx;
                side = 2;
            }
            else {
                rayCell.y += dySign;
                intersectionDistance = totalDistDy;
                totalDistDy += distDy;
                side = 1;
            }
            if (rayCell.x < 0 || rayCell.x >= mapCols
                    || rayCell.y < 0 || rayCell.y >= mapRows) break;

            if (map[rayCell.y][rayCell.x] == 1) {
                double ipx = src.x + intersectionDistance * dx;
                double ipy = src.y + intersectionDistance * dy;
                intersectionPoint.setLocation(ipx, ipy);
                return side;
            }
        }        
        return 0;
    }
    
    private void update() {
        final double speed = 0.02;
        if (keyDown[KeyEvent.VK_LEFT]) {
            player.x -= speed;
        }
        else if (keyDown[KeyEvent.VK_RIGHT]) {
            player.x += speed;
        }
        if (keyDown[KeyEvent.VK_UP]) {
            player.y -= speed;
        }
        else if (keyDown[KeyEvent.VK_DOWN]) {
            player.y += speed;
        }
    }
    
    private void drawMap(Graphics2D g) {
        g.setColor(Color.BLACK);
        for (int row = 0; row < map.length; row++) {
            int[] cols = map[row];
            for (int col = 0; col < cols.length; col++) {
                if (cols[col] == 1) {
                    g.fillRect(
                        col * TILE_SIZE, row * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                }
                else {
                    g.drawRect(
                        col * TILE_SIZE, row * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                }
            }
        }
    }

    private void drawPlayerMouseRay(Graphics2D g) {
        g.setColor(Color.BLUE);
        int x0 = (int) (player.x * TILE_SIZE);
        int y0 = (int) (player.y * TILE_SIZE);
        int x1 = (int) (mouse.x * TILE_SIZE);
        int y1 = (int) (mouse.y * TILE_SIZE);
        g.drawLine(x0, y0, x1, y1);
    }
    
    private void drawPlayer(Graphics2D g) {
        g.setColor(Color.MAGENTA);
        int x0 = (int) (player.x * TILE_SIZE);
        int y0 = (int) (player.y * TILE_SIZE);
        g.fillOval(x0 - 3, y0 - 3, 6, 6);
    }
    
    private final boolean[] keyDown = new boolean[256];
    
    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() < 256) {
            keyDown[e.getKeyCode()] = true;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() < 256) {
            keyDown[e.getKeyCode()] = false;
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        mouse.x = ((double) e.getX() / TILE_SIZE);
        mouse.y = ((double) e.getY() / TILE_SIZE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            RaycastingUsingDDATest ddaTest = new RaycastingUsingDDATest();
            ddaTest.start();
            JFrame frame = new JFrame();
            frame.setTitle("2D Raycasting using Digital "
                        + "Differential Analyzer (DDA) / Test #1");
            
            frame.getContentPane().add(ddaTest);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
            ddaTest.requestFocus();
        });
    }
    
}
