package io.rootvault.glassdialer;
final class Geometry {
 private Geometry(){}
 static float clamp(float v,float lo,float hi){return Math.max(lo,Math.min(hi,v));}
 static float radius(float width,float height,float dp){return Math.max(0,Math.min(36*dp,.425f*Math.min(width,height)));}
 static float spring(float t){if(t<=0)return 0;if(t>=1)return 1;return 1-(float)(Math.exp(-7.4*t)*Math.cos(9.3*t));}
 static float dragLeft(float x,float grab,float min,float max,float width){return clamp(x-grab,min,Math.max(min,max-width));}
 static int nearest(float x,float[] centers){int best=-1;float dist=Float.MAX_VALUE;for(int i=0;i<centers.length;i++){float d=Math.abs(x-centers[i]);if(d<dist){best=i;dist=d;}}return best;}
 static float projection(float center,float velocity,float dp){return center+clamp(velocity*.045f,-32*dp,32*dp);}
 static float[] haloStops(float edge){edge=clamp(edge,.01f,.91f);return new float[]{0,edge*.54f,edge*.78f,edge,Math.min(.97f,edge+.05f),1};}
}
