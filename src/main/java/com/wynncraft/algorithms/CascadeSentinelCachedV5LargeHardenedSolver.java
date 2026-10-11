package com.wynncraft.algorithms;

import com.wynncraft.core.interfaces.IAlgorithm;
import com.wynncraft.core.interfaces.IEquipment;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Exact scalar solver for snapshots outside the common fast domain, including
 * inputs with more than nine candidates or more than 64 items.
 *
 * <p>Uses sound reachability reductions and admissible exact-search bounds, with
 * a self-contained exact fallback beyond the bounded mask search. It stores no
 * reusable solve result between calls.</p>
 */
final class CascadeSentinelCachedV5LargeHardenedSolver implements IAlgorithm<CascadeSentinelCachedV5Player> {
    private static final int NO_NEED = Integer.MIN_VALUE;
    private static final int MAX_DIRECT_ITEMS = 28;
    private static final long MAX_SYMMETRY_REDUCED_STATES = 1_000_000L;

    private int[] originalIndex;
    private int[] r0,r1,r2,r3,r4;
    private int[] b0,b1,b2,b3,b4;
    private int[] p0,p1,p2,p3,p4;
    private int[] itemWeight;
    private int[] previousEquivalent = new int[32];
    private int[] branchOrder = new int[32];
    private int branchOrderCount;
    private int globalCardinalityUpper;
    private long[] visitedKeys = new long[256];
    private int[] visitedGen = new int[256];
    private int visitedEpoch = 1, visitedSize;

    private int k, itemCount, baseCount, baseScore;
    private long forcedOriginalMask, bestOriginalMask, fullSearchMask, allowedSearchMask;
    private int bestCount, bestScore;
    private int bestB0,bestB1,bestB2,bestB3,bestB4;
    private int alloc0,alloc1,alloc2,alloc3,alloc4;
    private boolean anyNegative;
    private long[] fallbackTTKeys;
    private int[] fallbackTTGen;
    private int fallbackTTEpoch = 1;

    @Override
    public Result run(CascadeSentinelCachedV5Player player) {
        final List<IEquipment> equipment = player;
        itemCount = player.itemCount;
        if (itemCount > 64) return fallback(player);
        CascadeSentinelCachedV5Player.Data d = player.data;
        CascadeSentinelCachedV5Player.HardenedData h = d.hardened;
        originalIndex=d.originalIndex; r0=h.r0;r1=h.r1;r2=h.r2;r3=h.r3;r4=h.r4;
        b0=h.b0;b1=h.b1;b2=h.b2;b3=h.b3;b4=h.b4;
        p0=h.p0;p1=h.p1;p2=h.p2;p3=h.p3;p4=h.p4; itemWeight=d.weight;
        alloc0=d.alloc0; alloc1=d.alloc1; alloc2=d.alloc2; alloc3=d.alloc3; alloc4=d.alloc4;
        CascadeSentinelCachedV5Player.HardenedPlayer snapshot = (CascadeSentinelCachedV5Player.HardenedPlayer) player;
        baseCount=snapshot.freeCount(); baseScore=snapshot.freeScore();
        forcedOriginalMask=player.freeOriginalMask(); k=player.candidateCount(); anyNegative=player.anyNegative();
        int s0=alloc0+snapshot.free0, s1=alloc1+snapshot.free1, s2=alloc2+snapshot.free2, s3=alloc3+snapshot.free3, s4=alloc4+snapshot.free4;

        if(k>MAX_DIRECT_ITEMS && !symmetryReducedDirectSafe()) return fallback(player);
        fullSearchMask = k==64?-1L:(1L<<k)-1L;
        allowedSearchMask = fullSearchMask;
        setBest(0L,0,baseScore,s0,s1,s2,s3,s4);

        if(k!=0){
            // A concrete valid order containing every candidate proves cardinality optimal.
            if(!tryGreedyAll(allowedSearchMask,s0,s1,s2,s3,s4)) {
                // Ignore negative bonuses to form an optimistic monotone relaxation.
                // Anything unreachable there is impossible in the exact problem.
                allowedSearchMask = optimisticReachableMask(s0,s1,s2,s3,s4);
                if(allowedSearchMask==0L) {
                    return finish(player,equipment);
                }

                // Retry the full constructive certificate after removing impossible candidates.
                if(!tryGreedyAll(allowedSearchMask,s0,s1,s2,s3,s4)) {
                    // A high-weight constructive pass only strengthens the incumbent;
                    // exact bounds still determine correctness.
                    tryWeightedGreedy(allowedSearchMask,s0,s1,s2,s3,s4);

                    globalCardinalityUpper = baseCount + finalCascadeCardinalityUpper(allowedSearchMask,s0,s1,s2,s3,s4);
                    if(bestCount>=globalCardinalityUpper){
                        int need=globalCardinalityUpper-baseCount;
                        if(bestScore>=baseScore+topWeightSum(allowedSearchMask,need))
                            return finish(player,equipment);
                    }

                    if(!hasNegativeInMask(allowedSearchMask)){
                        solvePositiveOnly(allowedSearchMask,s0,s1,s2,s3,s4);
                    } else {
                        prepareEquivalentItems();
                        beginVisited();
                        search(0L,0,baseScore,s0,s1,s2,s3,s4,NO_NEED,NO_NEED,NO_NEED,NO_NEED,NO_NEED);
                    }
                }
            }
        }

        return finish(player,equipment);

    }

    private Result finish(CascadeSentinelCachedV5Player player, List<IEquipment> equipment) {
        player.setBonus(bestB0,bestB1,bestB2,bestB3,bestB4);
        return result(equipment);
    }

    @Override
    public void clearCache() {
        // No persistent solve result to clear.
    }

    private boolean tryGreedyAll(long allowed,int s0,int s1,int s2,int s3,int s4){
        long mask=0L, remaining=allowed; int chosen=0,score=baseScore;
        int n0=NO_NEED,n1=NO_NEED,n2=NO_NEED,n3=NO_NEED,n4=NO_NEED;
        boolean progress;
        do{
            progress=false; long c=remaining;
            while(c!=0L){
                long bit=c&-c; c^=bit; int i=Long.numberOfTrailingZeros(bit);
                if(!canEquip(i,s0,s1,s2,s3,s4)) continue;
                int t0=s0+b0[i],t1=s1+b1[i],t2=s2+b2[i],t3=s3+b3[i],t4=s4+b4[i];
                if(t0<n0||t1<n1||t2<n2||t3<n3||t4<n4) continue;
                mask|=bit; remaining^=bit; chosen++; score+=itemWeight[i];
                s0=t0;s1=t1;s2=t2;s3=t3;s4=t4;
                if(p0[i]>n0)n0=p0[i]; if(p1[i]>n1)n1=p1[i]; if(p2[i]>n2)n2=p2[i]; if(p3[i]>n3)n3=p3[i]; if(p4[i]>n4)n4=p4[i];
                progress=true;
            }
        }while(progress&&remaining!=0L);
        if(remaining==0L){ setBest(mask,chosen,score,s0,s1,s2,s3,s4); return true; }
        if(baseCount+chosen>bestCount || (baseCount+chosen==bestCount && score>bestScore))
            setBest(mask,chosen,score,s0,s1,s2,s3,s4);
        return false;
    }

    private void solvePositiveOnly(long allowed,int s0,int s1,int s2,int s3,int s4){
        long mask=0L, remaining=allowed; int chosen=0,score=baseScore;
        boolean progress;
        do{
            progress=false; long c=remaining;
            while(c!=0L){ long bit=c&-c;c^=bit;int i=Long.numberOfTrailingZeros(bit); if(!canEquip(i,s0,s1,s2,s3,s4))continue;
                mask|=bit;remaining^=bit;chosen++;score+=itemWeight[i];s0+=b0[i];s1+=b1[i];s2+=b2[i];s3+=b3[i];s4+=b4[i];progress=true; }
        }while(progress&&remaining!=0L);
        setBest(mask,chosen,score,s0,s1,s2,s3,s4);
    }

    private boolean search(long mask,int chosen,int score,
                           int s0,int s1,int s2,int s3,int s4,int n0,int n1,int n2,int n3,int n4){
        int count=baseCount+chosen;
        if(count>bestCount||(count==bestCount&&score>bestScore)){ setBest(mask,chosen,score,s0,s1,s2,s3,s4); if(count==itemCount)return true; }

        // Bound cardinality by the global relaxed limit and the number of
        // unselected allowed candidates. This path does not recompute an
        // optimistic closure or a tie-weight bound at each search node.
        long candidates=allowedSearchMask & ~mask;
        int rem=Long.bitCount(candidates), max=Math.min(globalCardinalityUpper,count+rem);
        if(max<bestCount) return false;
        if(max==bestCount){
            int need=bestCount-count;
            if(need<=0) return false;
        }

        for(int oi=0;oi<branchOrderCount;oi++){
            int i=branchOrder[oi]; long bit=1L<<i;
            if((candidates&bit)==0L) continue;
            int prev=previousEquivalent[i];
            if(prev>=0 && (mask&(1L<<prev))==0L) continue;
            if(!canEquip(i,s0,s1,s2,s3,s4))continue;
            int t0=s0+b0[i],t1=s1+b1[i],t2=s2+b2[i],t3=s3+b3[i],t4=s4+b4[i];
            if(t0<n0||t1<n1||t2<n2||t3<n3||t4<n4)continue;
            long next=mask|bit; if(!markVisited(next))continue;
            int nn0=p0[i]>n0?p0[i]:n0, nn1=p1[i]>n1?p1[i]:n1, nn2=p2[i]>n2?p2[i]:n2, nn3=p3[i]>n3?p3[i]:n3, nn4=p4[i]>n4?p4[i]:n4;
            if(search(next,chosen+1,score+itemWeight[i],t0,t1,t2,t3,t4,nn0,nn1,nn2,nn3,nn4))return true;
        }
        return false;
    }

    private void tryWeightedGreedy(long allowed,int s0,int s1,int s2,int s3,int s4){
        long mask=0L, remaining=allowed; int chosen=0,score=baseScore;
        int n0=NO_NEED,n1=NO_NEED,n2=NO_NEED,n3=NO_NEED,n4=NO_NEED;
        while(remaining!=0L){
            int best=-1,bestW=Integer.MIN_VALUE; long c=remaining;
            while(c!=0L){long bit=c&-c;c^=bit;int i=Long.numberOfTrailingZeros(bit);
                if(itemWeight[i]<bestW || !canEquip(i,s0,s1,s2,s3,s4))continue;
                int t0=s0+b0[i],t1=s1+b1[i],t2=s2+b2[i],t3=s3+b3[i],t4=s4+b4[i];
                if(t0<n0||t1<n1||t2<n2||t3<n3||t4<n4)continue;
                best=i;bestW=itemWeight[i];}
            if(best<0)break; long bit=1L<<best; remaining^=bit;mask|=bit;chosen++;score+=itemWeight[best];
            s0+=b0[best];s1+=b1[best];s2+=b2[best];s3+=b3[best];s4+=b4[best];
            if(p0[best]>n0)n0=p0[best];if(p1[best]>n1)n1=p1[best];if(p2[best]>n2)n2=p2[best];if(p3[best]>n3)n3=p3[best];if(p4[best]>n4)n4=p4[best];
        }
        if(baseCount+chosen>bestCount || (baseCount+chosen==bestCount&&score>bestScore))setBest(mask,chosen,score,s0,s1,s2,s3,s4);
    }

    private int finalCascadeCardinalityUpper(long mask,int s0,int s1,int s2,int s3,int s4){
        int all=Long.bitCount(mask);
        int upper=all;
        upper=Math.min(upper,oneDimCardinalityUpper(mask,s0,b0,p0));
        upper=Math.min(upper,oneDimCardinalityUpper(mask,s1,b1,p1));
        upper=Math.min(upper,oneDimCardinalityUpper(mask,s2,b2,p2));
        upper=Math.min(upper,oneDimCardinalityUpper(mask,s3,b3,p3));
        upper=Math.min(upper,oneDimCardinalityUpper(mask,s4,b4,p4));
        return upper;
    }

    private int oneDimCardinalityUpper(long mask,int base,int[] bonus,int[] need){
        int best=0;
        // No selected item requires this skill: all no-need items are unconstrained here.
        long c=mask; while(c!=0L){long bit=c&-c;c^=bit;int i=Long.numberOfTrailingZeros(bit);if(need[i]==NO_NEED)best++;}
        // Every finite need is a sufficient threshold candidate for the 1-D relaxation.
        c=mask;
        while(c!=0L){
            long tb=c&-c;c^=tb;int ti=Long.numberOfTrailingZeros(tb);int threshold=need[ti];if(threshold==NO_NEED)continue;
            long eligible=0L;long sum=base;int count=0;long x=mask;
            while(x!=0L){long bit=x&-x;x^=bit;int i=Long.numberOfTrailingZeros(bit);if(need[i]==NO_NEED||need[i]<=threshold){eligible|=bit;sum+=bonus[i];count++;}}
            if(sum<threshold){
                while(sum<threshold){
                    int worst=-1,worstBonus=0;long y=eligible;
                    while(y!=0L){long bit=y&-y;y^=bit;int i=Long.numberOfTrailingZeros(bit);if(bonus[i]<worstBonus){worstBonus=bonus[i];worst=i;}}
                    if(worst<0)break; eligible&=~(1L<<worst);sum-=worstBonus;count--;
                }
            }
            if(sum>=threshold && count>best)best=count;
        }
        return best;
    }

    private int topWeightSum(long mask,int count){
        if(count<=0)return 0;
        int sum=0;
        // The bounded direct-search path is small; repeated max selection is allocation-free.
        long left=mask;
        for(int q=0;q<count && left!=0L;q++){
            int best=-1,bw=Integer.MIN_VALUE;long c=left;
            while(c!=0L){long bit=c&-c;c^=bit;int i=Long.numberOfTrailingZeros(bit);if(itemWeight[i]>bw){bw=itemWeight[i];best=i;}}
            if(best<0)break;sum+=bw;left&=~(1L<<best);
        }
        return sum;
    }

    private void prepareEquivalentItems(){
        if(previousEquivalent.length<k) previousEquivalent=new int[k];
        if(branchOrder.length<k) branchOrder=new int[k];
        Arrays.fill(previousEquivalent,0,k,-1); branchOrderCount=0;
        for(int i=0;i<k;i++) if((allowedSearchMask&(1L<<i))!=0L){
            if(i>0) for(int j=i-1;j>=0;j--){
                if((allowedSearchMask&(1L<<j))==0L) continue;
                if(sameProfile(i,j)){ previousEquivalent[i]=j; break; }
            }
            int pos=branchOrderCount;
            while(pos>0 && itemWeight[branchOrder[pos-1]]<itemWeight[i]){branchOrder[pos]=branchOrder[pos-1];pos--;}
            branchOrder[pos]=i;branchOrderCount++;
        }
    }

    private boolean sameProfile(int i,int j){
        return r0[i]==r0[j]&&r1[i]==r1[j]&&r2[i]==r2[j]&&r3[i]==r3[j]&&r4[i]==r4[j]
                &&b0[i]==b0[j]&&b1[i]==b1[j]&&b2[i]==b2[j]&&b3[i]==b3[j]&&b4[i]==b4[j];
    }

    /**
     * Above the direct-search ceiling, use the mask engine only when equivalence
     * classes bound the number of canonical subset states to at most the configured
     * cap. This admits large duplicate-heavy inputs without exposing arbitrary
     * high-cardinality inputs to an unrestricted 2^k state space.
     */
    private boolean symmetryReducedDirectSafe(){
        long states = 1L;
        for(int i=0;i<k;i++){
            boolean leader = true;
            for(int j=0;j<i;j++){
                if(sameProfile(i,j)){ leader=false; break; }
            }
            if(!leader) continue;
            int group = 1;
            for(int j=i+1;j<k;j++) if(sameProfile(i,j)) group++;
            long factor = (long)group + 1L;
            if(states > MAX_SYMMETRY_REDUCED_STATES / factor) return false;
            states *= factor;
        }
        return states <= MAX_SYMMETRY_REDUCED_STATES;
    }

    private long optimisticRemainingMask(long remaining,int s0,int s1,int s2,int s3,int s4){
        long reached=0L;
        boolean progress;
        do {
            progress=false;
            long c=remaining;
            while(c!=0L){
                long bit=c&-c; c^=bit; int i=Long.numberOfTrailingZeros(bit);
                if(!canEquip(i,s0,s1,s2,s3,s4)) continue;
                reached|=bit; remaining^=bit; progress=true;
                if(b0[i]>0)s0+=b0[i]; if(b1[i]>0)s1+=b1[i]; if(b2[i]>0)s2+=b2[i];
                if(b3[i]>0)s3+=b3[i]; if(b4[i]>0)s4+=b4[i];
            }
        } while(progress && remaining!=0L);
        return reached;
    }

    private long optimisticReachableMask(int s0,int s1,int s2,int s3,int s4){
        long reached=0L, remaining=fullSearchMask;
        boolean progress;
        do {
            progress=false;
            long c=remaining;
            while(c!=0L){
                long bit=c&-c; c^=bit; int i=Long.numberOfTrailingZeros(bit);
                if(!canEquip(i,s0,s1,s2,s3,s4)) continue;
                reached|=bit; remaining^=bit; progress=true;
                if(b0[i]>0)s0+=b0[i]; if(b1[i]>0)s1+=b1[i]; if(b2[i]>0)s2+=b2[i];
                if(b3[i]>0)s3+=b3[i]; if(b4[i]>0)s4+=b4[i];
            }
        } while(progress && remaining!=0L);
        return reached;
    }

    private boolean hasNegativeInMask(long mask){
        long c=mask;
        while(c!=0L){
            long bit=c&-c; c^=bit; int i=Long.numberOfTrailingZeros(bit);
            if(b0[i]<0||b1[i]<0||b2[i]<0||b3[i]<0||b4[i]<0)return true;
        }
        return false;
    }

    private int weightOfMask(long mask){
        int w=0; long c=mask;
        while(c!=0L){long bit=c&-c;c^=bit;w+=itemWeight[Long.numberOfTrailingZeros(bit)];}
        return w;
    }

    private boolean canEquip(int i,int s0,int s1,int s2,int s3,int s4){
        return (r0[i]<=0||s0>=r0[i])&&(r1[i]<=0||s1>=r1[i])&&(r2[i]<=0||s2>=r2[i])&&(r3[i]<=0||s3>=r3[i])&&(r4[i]<=0||s4>=r4[i]);
    }

    private void setBest(long searchMask,int chosen,int score,int s0,int s1,int s2,int s3,int s4){
        bestCount=baseCount+chosen;bestScore=score;long om=forcedOriginalMask;long x=searchMask;
        while(x!=0L){long bit=x&-x;x^=bit;om|=1L<<originalIndex[Long.numberOfTrailingZeros(bit)];}
        bestOriginalMask=om;bestB0=s0-alloc0;bestB1=s1-alloc1;bestB2=s2-alloc2;bestB3=s3-alloc3;bestB4=s4-alloc4;
    }

    private Result result(List<IEquipment> equipment){
        if(bestCount==itemCount)return new Result(equipment,Collections.emptyList());
        if(bestCount==0)return new Result(Collections.emptyList(),equipment);
        long all=itemCount==64?-1L:(1L<<itemCount)-1L;
        return new Result(new MaskList(equipment,bestOriginalMask,bestCount),new MaskList(equipment,all&~bestOriginalMask,itemCount-bestCount));
    }

    private Result fallback(CascadeSentinelCachedV5Player player){
        // Exact fallback outside the bounded mask search. Keep it self-contained so
        // correctness, including tie-breaking, does not depend on another registered algorithm.
        // This general fallback reads original item values, which must stay stable
        // for the duration of the solve; the bounded paths use captured sidecars.
        final int n=player.itemCount;
        IEquipment[] items=Arrays.copyOf(player.data.items,n);
        int[] sp={player.data.alloc0,player.data.alloc1,player.data.alloc2,player.data.alloc3,player.data.alloc4};
        boolean[] active=new boolean[n], best=new boolean[n];
        int count=0,score=0,remaining=0;
        for(int i=0;i<n;i++){
            int[] req=items[i].requirements(), bonus=items[i].bonuses();
            boolean hasReq=false,hasNeg=false;
            for(int s=0;s<5;s++){if(req[s]>0)hasReq=true;if(bonus[s]<0)hasNeg=true;}
            if(!hasReq&&!hasNeg){active[i]=true;count++;int w=0;for(int s=0;s<5;s++){sp[s]+=bonus[s];w+=bonus[s];}score+=w;}
            else remaining++;
        }
        FallbackBest fb=new FallbackBest(n,count,score,active);
        if(n<=64){
            beginFallbackTT();
            long mask=0L; for(int i=0;i<n;i++) if(active[i]) mask|=1L<<i;
            recordFallbackState(mask);
            fallbackSearchMasked(items,sp,active,count,score,remaining,fb,mask);
        } else {
            fallbackSearch(items,sp,active,count,score,remaining,fb);
        }
        player.reset();
        for(int i=0;i<n;i++)if(fb.best[i])player.modify(items[i].bonuses(),true);
        if(fb.count==n)return new Result(player,Collections.emptyList());
        List<IEquipment> valid=new java.util.ArrayList<>(fb.count), invalid=new java.util.ArrayList<>(n-fb.count);
        for(int i=0;i<n;i++)(fb.best[i]?valid:invalid).add(items[i]);
        return new Result(valid,invalid);
    }

    private void beginFallbackTT(){
        final int size=1<<16;
        if(fallbackTTKeys==null){fallbackTTKeys=new long[size];fallbackTTGen=new int[size];}
        if(++fallbackTTEpoch==0){Arrays.fill(fallbackTTGen,0);fallbackTTEpoch=1;}
    }
    private boolean recordFallbackState(long key){
        int slot=mix(key)&(fallbackTTKeys.length-1);
        if(fallbackTTGen[slot]==fallbackTTEpoch && fallbackTTKeys[slot]==key)return false;
        fallbackTTGen[slot]=fallbackTTEpoch; fallbackTTKeys[slot]=key; return true;
    }
    private void fallbackSearchMasked(IEquipment[] items,int[] sp,boolean[] active,
                                      int count,int score,int remaining,FallbackBest best,long mask){
        if(count>best.count||(count==best.count&&score>best.score)){
            best.count=count;best.score=score;System.arraycopy(active,0,best.best,0,active.length);
            if(count==items.length)return;
        }
        if(count+remaining<best.count)return;
        for(int i=0;i<items.length;i++){
            if(active[i])continue;
            int[] req=items[i].requirements(); boolean meets=true;
            for(int q=0;q<5;q++)if(req[q]>0&&sp[q]<req[q]){meets=false;break;}
            if(!meets)continue;
            int[] bonus=items[i].bonuses(); for(int q=0;q<5;q++)sp[q]+=bonus[q];
            boolean sustainable=true;
            for(int j=0;j<items.length&&sustainable;j++)if(active[j]){
                int[] r=items[j].requirements(),b=items[j].bonuses();
                for(int q=0;q<5;q++)if(r[q]>0&&sp[q]-b[q]<r[q]){sustainable=false;break;}
            }
            if(sustainable){
                active[i]=true; int w=bonus[0]+bonus[1]+bonus[2]+bonus[3]+bonus[4]; long next=mask|(1L<<i);
                if(recordFallbackState(next)) fallbackSearchMasked(items,sp,active,count+1,score+w,remaining-1,best,next);
                active[i]=false;
                if(best.count==items.length){for(int q=0;q<5;q++)sp[q]-=bonus[q];return;}
            }
            for(int q=0;q<5;q++)sp[q]-=bonus[q];
        }
    }

    private static void fallbackSearch(IEquipment[] items,int[] sp,boolean[] active,
                                       int count,int score,int remaining,FallbackBest best){
        if(count>best.count||(count==best.count&&score>best.score)){
            best.count=count;best.score=score;System.arraycopy(active,0,best.best,0,active.length);
            if(count==items.length)return;
        }
        // Strictly less: equality can still tie cardinality and improve total
        // given skill points, so it must remain searchable.
        if(count+remaining<best.count)return;
        for(int i=0;i<items.length;i++){
            if(active[i])continue;
            int[] req=items[i].requirements();
            boolean meets=true;
            for(int s=0;s<5;s++)if(req[s]>0&&sp[s]<req[s]){meets=false;break;}
            if(!meets)continue;
            int[] bonus=items[i].bonuses();
            for(int s=0;s<5;s++)sp[s]+=bonus[s];
            boolean sustainable=true;
            for(int j=0;j<items.length&&sustainable;j++)if(active[j]){
                int[] r=items[j].requirements(),b=items[j].bonuses();
                for(int s=0;s<5;s++)if(r[s]>0&&sp[s]-b[s]<r[s]){sustainable=false;break;}
            }
            if(sustainable){
                active[i]=true;int w=bonus[0]+bonus[1]+bonus[2]+bonus[3]+bonus[4];
                fallbackSearch(items,sp,active,count+1,score+w,remaining-1,best);
                active[i]=false;
                if(best.count==items.length){for(int s=0;s<5;s++)sp[s]-=bonus[s];return;}
            }
            for(int s=0;s<5;s++)sp[s]-=bonus[s];
        }
    }

    private static final class FallbackBest{
        int count,score;final boolean[] best;
        FallbackBest(int n,int count,int score,boolean[] active){this.count=count;this.score=score;this.best=Arrays.copyOf(active,n);}
    }

    private void beginVisited(){
        visitedSize=0;
        if(++visitedEpoch==0){Arrays.fill(visitedGen,0);visitedEpoch=1;}
    }

    private boolean markVisited(long key){
        if((visitedSize+1)*10 >= visitedKeys.length*7) growVisited();
        int mask=visitedKeys.length-1; int slot=mix(key)&mask;
        while(visitedGen[slot]==visitedEpoch){
            if(visitedKeys[slot]==key)return false;
            slot=(slot+1)&mask;
        }
        visitedGen[slot]=visitedEpoch; visitedKeys[slot]=key; visitedSize++; return true;
    }

    private void growVisited(){
        long[] oldKeys=visitedKeys; int[] oldGen=visitedGen; int oldEpoch=visitedEpoch;
        visitedKeys=new long[oldKeys.length<<1]; visitedGen=new int[visitedKeys.length];
        visitedEpoch=1; visitedSize=0;
        int mask=visitedKeys.length-1;
        for(int i=0;i<oldKeys.length;i++) if(oldGen[i]==oldEpoch){
            long key=oldKeys[i]; int slot=mix(key)&mask;
            while(visitedGen[slot]==visitedEpoch)slot=(slot+1)&mask;
            visitedGen[slot]=visitedEpoch;visitedKeys[slot]=key;visitedSize++;
        }
    }

    private static int mix(long x){
        x^=x>>>33; x*=0xff51afd7ed558ccdl; x^=x>>>33;
        return (int)x;
    }

    private static final class MaskList extends AbstractList<IEquipment>{
        private final List<IEquipment> equipment;private final long mask;private final int size;
        MaskList(List<IEquipment> equipment,long mask,int size){this.equipment=equipment;this.mask=mask;this.size=size;}
        @Override public IEquipment get(int index){if(index<0||index>=size)throw new IndexOutOfBoundsException(index);long m=mask;for(int i=0;i<index;i++)m&=m-1;return equipment.get(Long.numberOfTrailingZeros(m));}
        @Override public int size(){return size;}
    }
}
