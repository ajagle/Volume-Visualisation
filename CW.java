import java.io.DataInputStream;
import java.io.FileInputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.lang.Math;
import java.awt.Color;

class Volume
{
    int data[][][];
    double zoom=1.0;
    int resolution=512;
    int val_amplication=5;

    /**
     * This function reads a volume dataset from disk and put the result in the data array
     * @param //amplification allows increasing the brightness of the slice by a constant.
     */
    public int GetResolution()
    {
        return resolution;
    }

    public void SetResolution(int res)
    {
        resolution=res;
    }

    public double GetZoom()
    {
        return zoom;
    }

    public void SetZoom(float z)
    {
        zoom=z;
    }

    boolean ReadData(String fileName, int sizeX, int sizeY, int sizeZ, int headerSize)
    {
        int cpt=0;
        byte dataBytes[]=new byte [sizeX*sizeY*sizeZ+headerSize];
        data = new int[sizeZ][sizeY][sizeX];
        try
        {
            FileInputStream f = new FileInputStream(fileName);
            DataInputStream d = new DataInputStream(f);

            d.readFully(dataBytes);

            //Copying the byte values into the floating-point array

            for (int k=0;k<sizeZ;k++)
                for (int j=0;j<sizeY;j++)
                    for (int i=0;i<sizeX;i++)
                        data[k][j][i]=dataBytes[k*256*sizeY+j*sizeX+i+headerSize] & 0xff;
        }
        catch(Exception e)
        {
            System.out.println("Exception : "+cpt+e);
            return false;
        }
        return true;
    }



    /**
     * This function returns the 3D gradient for the volumetric dataset (data variable). Note that the gradient values at the sides of the volume is not be computable. Each cell element containing a 3D vector, the result is therefore a 4D array.
     */
    int [][][][] Gradient()
    {
        int[][][][] gradient=null;
        int dimX=data[0][0].length;
        int dimY=data[0].length;
        int dimZ=data.length;
        gradient=new int[dimZ-2][dimY-2][dimX-2][3]; //-2 due gradient not being computable at borders
        for (int k = 1; k < dimZ-1; k++)
            for (int j = 1; j < dimY-1; j++)
                for (int i = 1; i < dimX-1; i++)
                {
                    gradient[k-1][j-1][i-1][0]=(data[k][j][i+1]-data[k][j][i-1])/2;
                    gradient[k-1][j-1][i-1][1]=(data[k][j+1][i]-data[k][j-1][i])/2;
                    gradient[k-1][j-1][i-1][2]=(data[k+1][j][i]-data[k-1][j][i])/2;
                }
        return gradient;
    }


    /**
     * This function returns an image of a contour visualisation projected along the z axis.
     * @param //direction The direction of the ray along the axis
     * @param //isovalue The threshold value for delimitating the isosurface. Does not have to be used at all
     */

    private double safeTrilinearInterpolation(double x, double y, double z) {
        // Clamp to volume bounds with better floating-point precision handling
        x = Math.max(0, Math.min(Math.nextDown(data[0][0].length - 1), x));
        y = Math.max(0, Math.min(Math.nextDown(data[0].length - 1), y));
        z = Math.max(0, Math.min(Math.nextDown(data.length - 1), z));

        int x0 = (int) x;
        int y0 = (int) y;
        int z0 = (int) z;
        int x1 = Math.min(x0 + 1, data[0][0].length - 1);
        int y1 = Math.min(y0 + 1, data[0].length - 1);
        int z1 = Math.min(z0 + 1, data.length - 1);

        double xd = x - x0;
        double yd = y - y0;
        double zd = z - z0;

        // Interpolate along X-axis
        double c00 = data[z0][y0][x0] * (1 - xd) + data[z0][y0][x1] * xd;
        double c01 = data[z0][y1][x0] * (1 - xd) + data[z0][y1][x1] * xd;
        double c10 = data[z1][y0][x0] * (1 - xd) + data[z1][y0][x1] * xd;
        double c11 = data[z1][y1][x0] * (1 - xd) + data[z1][y1][x1] * xd;

        // Interpolate along Y-axis
        double c0 = c00 * (1 - yd) + c01 * yd;
        double c1 = c10 * (1 - yd) + c11 * yd;

        // Final interpolation along Z-axis
        return c0 * (1 - zd) + c1 * zd;
    }


    // Contour rendering function (Modified)
    public int[][] RenderContour(int[][][][] gradient, int isovalue, boolean positiveDirection) {
        int dimX = data[0][0].length;
        int dimY = data[0].length;
        int dimZ = data.length;
        int[][] contourImage = new int[resolution][resolution];

        float stepSize = 0.05f;  // Finer steps for smoother results
        float halfRes = resolution / 2.0f;

        double scaleX = (dimX / (double) resolution) * zoom;
        double scaleY = (dimY / (double) resolution) * zoom;

        int gradDimZ = gradient.length;
        int gradDimY = gradient[0].length;
        int gradDimX = gradient[0][0].length;

        for (int j = 0; j < resolution; j++) {
            for (int i = 0; i < resolution; i++) {
                double x = (i - halfRes) * scaleX + dimX / 2;
                double y = (j - halfRes) * scaleY + dimY / 2;
                float z = 1.0f;

                x = Math.max(1, Math.min(gradDimX - 2, x));
                y = Math.max(1, Math.min(gradDimY - 2, y));

                int maxIntensity = 0;
                double prevValue = safeTrilinearInterpolation(x, y, z);

                while (z < gradDimZ - 1) {
                    double value = safeTrilinearInterpolation(x, y, z);

                    int gz = (int) z;
                    int gy = (int) y;
                    int gx = (int) x;

                    if (gz >= 0 && gz < gradDimZ && gy >= 0 && gy < gradDimY && gx >= 0 && gx < gradDimX) {
                        float gx_val = gradient[gz][gy][gx][0];
                        float gy_val = gradient[gz][gy][gx][1];
                        float gz_val = gradient[gz][gy][gx][2];
                        float magnitude = (float) Math.sqrt(gx_val * gx_val + gy_val * gy_val + gz_val * gz_val);
                        float threshold = 9.0f; // Lower threshold to capture softer contours
                        // Compute base contour strength
                        float contourStrength = magnitude * val_amplication;
                        if ((prevValue - isovalue) * (value - isovalue) < 0) {
                            // Linear interpolation for zero-crossing detection
                            float alpha = (float) ((isovalue - prevValue) / (value - prevValue + 1e-6));

                            // Adjust contour strength dynamically
                            contourStrength *= Math.pow((1.5 - alpha) * 1.2, 1.2);

                            // Apply dynamic thresholding to avoid artifacts
                            if (contourStrength > threshold) {
                                maxIntensity = Math.max(maxIntensity, Math.min(256, (int) (contourStrength * 1.9)));
                            }
                        } else {
                            maxIntensity = Math.max(maxIntensity, (int) contourStrength);
                        }
                    }

                    prevValue = value;
                    z += stepSize;
                }

                contourImage[j][i] = Math.min(maxIntensity, 226);
            }
        }

        return contourImage;
    }



    /**
     * This function swaps the x or y dimension with the z one, allowing projection on other faces of the volume.
     */
    void SwapZAxis(int axis)
    {
        if (axis==2)
            return;
        int dimX=data[0][0].length;
        int dimY=data[0].length;
        int dimZ=data.length;
        int newvol[][][];
        if (axis==0)
        {
            newvol=new int[dimX][dimY][dimZ];
            for (int k = 0; k < dimZ; k++)
                for (int j = 0; j < dimY; j++)
                    for (int i = 0; i < dimX; i++)
                        newvol[i][j][k]=data[k][j][i];
        }
        else
        {
            newvol=new int[dimY][dimZ][dimX];
            for (int k = 0; k < dimZ; k++)
                for (int j = 0; j < dimY; j++)
                    for (int i = 0; i < dimX; i++)
                        newvol[j][k][i]=data[k][j][i];
        }
        data=newvol;
    }
}

public class CW
{
    /**
     * This function returns your name. Needs to be updated.
     */
    public static String Name()
    {
        return "You have entered your student number";
    }

    /**
     * This function returns your student id. Needs to be updated.
     */
    public static int SUID()
    {
        return 2449685;
    }

    public static void SaveImage(String name, int[][] im)
    {
        BufferedImage image = new BufferedImage(im.length, im[0].length, BufferedImage.TYPE_BYTE_GRAY );
        for (int j = 0; j < im.length; j++)
            for (int i = 0; i < im[0].length; i++)
                image.setRGB(j, i, im[j][i]*256*256+im[j][i]*256+im[j][i]);

        File f = new File(name);
        try
        {
            ImageIO.write(image, "tiff", f);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public static void SaveImageRGB(String name, int[][][] im)
    {
        BufferedImage image = new BufferedImage(im.length, im[0].length, BufferedImage.TYPE_INT_RGB );
        for (int j = 0; j < im.length; j++)
            for (int i = 0; i < im[0].length; i++)
            {
                Color c=new Color(Math.abs(im[j][i][0]),Math.abs(im[j][i][1]),Math.abs(im[j][i][2]));
                image.setRGB(j, i, c.getRGB());
            }

        File f = new File(name);
        try
        {
            ImageIO.write(image, "tiff", f);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    //The main function should not really be modified, except maybe for setting the zoom value and res values
    public static void main(String[] args)
    {
        System.out.println(Name());
        System.out.println(SUID());
        //Args: width height depth header_size isovalue projection_axis direction resolution zoom
        //A command line example: java CW 256 256 225 62 95 0 false 512 2.0
        Volume v=new Volume();
        v.ReadData("./bighead_den256X256X225B62H.raw",Integer.parseInt(args[0]),Integer.parseInt(args[1]),Integer.parseInt(args[2]),Integer.parseInt(args[3]));
        v.SwapZAxis(Integer.parseInt(args[5]));
        int[][][][] gradient =v.Gradient();
        int [][]im;
        im=v.RenderContour(gradient,Integer.parseInt(args[4]),Boolean.parseBoolean(args[6]));
        if (im!=null)
        {
            SaveImage("contour.tiff",im);
            SaveImage("contour"+SUID()+".tiff",im);
        }
    }
}