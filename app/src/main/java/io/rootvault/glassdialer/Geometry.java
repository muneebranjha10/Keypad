package io.rootvault.glassdialer;
final class Geometry {
 private Geometry(){}
 static float clamp(float v,float lo,float hi){return Math.max(lo,Math.min(hi,v));}
 static float radius(float width,float height,float dp){return Math.max(0,Math.min(36*dp,.425f*Math.min(width,height)));}
 static boolean appended(CharSequence s,int st,int old,int count){return s!=null&&old==0&&count==1&&st>=0&&st==s.length()-1&&"0123456789*#+".indexOf(s.charAt(st))>=0;}
 static boolean matchesAppend(CharSequence s,int index,char expected){return s!=null&&index>=0&&index==s.length()-1&&s.charAt(index)==expected;}
 static float safeRadius(float w,float h,float dp){float a=Math.min(w,h);return Math.max(0,Math.min(radius(w,h,dp),a/2.04f-Math.min(6*dp,a*.12f)));}
 static float recoilScale(float t){return 1f-.13f*clamp(t,-.13f,1f);}
 static int dialCount(CharSequence s){int n=0;if(s!=null)for(int i=0;i<s.length();i++)if(Character.isDigit(s.charAt(i))||s.charAt(i)=='*'||s.charAt(i)=='#'||s.charAt(i)=='+')n++;return n;}
 static float fitLeft(float center,float width,float min,float max){return clamp(center-width*.5f,min,Math.max(min,max-width));}
 static float centerShift(float min,float max,float size,float limit){if(!Float.isFinite(min)||!Float.isFinite(max)||max<min||max-min>size)return 0;float d=(size-min-max)*.5f;return Math.abs(d)<=limit?clamp(d,-min,size-max):0;}
 static boolean hit(float x,float y,float w,float h,float slop){return Float.isFinite(x)&&Float.isFinite(y)&&w>0&&h>0&&x>=-slop&&y>=-slop&&x<=w+slop&&y<=h+slop;}
 static float spring(float t){if(t<=0)return 0;if(t>=1)return 1;return 1-(float)(Math.exp(-7.4*t)*Math.cos(9.3*t));}
 static float dragLeft(float x,float grab,float min,float max,float width){return clamp(x-grab,min,Math.max(min,max-width));}
 static int nearest(float x,float[] centers){int best=-1;float dist=Float.MAX_VALUE;for(int i=0;i<centers.length;i++){float d=Math.abs(x-centers[i]);if(d<dist){best=i;dist=d;}}return best;}
 static float projection(float center,float velocity,float dp){return center+clamp(velocity*.045f,-32*dp,32*dp);}
 static float[] haloStops(float edge){edge=clamp(edge,.01f,.91f);return new float[]{0,edge*.54f,edge*.78f,edge,Math.min(.97f,edge+.05f),1};}
}
