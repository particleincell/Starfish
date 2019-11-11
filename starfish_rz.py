#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Thu Nov  7 09:15:54 2019

@author: tommyesse
"""

# axisymmetric (RZ) particle in cell code example
#
# see https://www.particleincell.com/2015/rz-pic/ for more info
# simulates a simplistic ion source in which ions are
# produced from a volumetric source with constant electron and 
# neutral density (i.e. not taking into account avalanche ionization
# or source depletion)
#
# code illustrates velocity and position rotation in RZ
#
# requires numpy, scipy, pylab, and mathplotlib

import numpy
import pylab as pl
import math
from random import (seed, random)

def XtoL(pos):
    lc = [pos[0]/dz, pos[1]/dr]
    return lc

def Pos(lc):
    pos = [lc[0]*dz,lc[1]*dr]
    return pos
 
def R(j):
    return j*dr
 
def gather(data,lc):
    i = math.trunc(lc[0])
    j = math.trunc(lc[1])
    di = lc[0] - i
    dj = lc[1] - j
    return  (data[i][j]*(1-di)*(1-dj) +
          data[i+1][j]*(di)*(1-dj) + 
          data[i][j+1]*(1-di)*(dj) + 
          data[i+1][j+1]*(di)*(dj)) 
    
def scatter(data,lc,value):
    i = int(numpy.trunc(lc[0]))
    j = int(numpy.trunc(lc[1]))
    di = lc[0] - i
    dj = lc[1] - j
            
    data[i][j] += (1-di)*(1-dj)*value
    data[i+1][j] += (di)*(1-dj)*value
    data[i][j+1] += (1-di)*(dj)*value
    data[i+1][j+1] += (di)*(dj)*value
       
#particle definition
class Particle:
    def __init__(self,pos,vel):
        self.pos=[pos[0],pos[1],0]    #xyz position
        self.vel=[vel[0],vel[1],vel[2]]

# --- helper functions ----
def sampleIsotropicVel(vth):
    #pick a random angle
    theta = 2*math.pi*random()
    
    #pick a random direction for n[2]
    R = -1.0+2*random()
    a = math.sqrt(1-R*R)
    n = (math.cos(theta)*a, math.sin(theta)*a, R)
    
    #pick maxwellian velocities
    vm = numpy.zeros(3)
    vm[0:3] = math.sqrt(2)*vth*(2*(random()+random()+random()-1.5))
    
    vel = (n[0]*vm[0], n[1]*vm[1], n[2]*vm[2]) 
    return vel

#simple Jacobian solver, does not do any convergence checking
def solvePotential(phi,max_it=100):
    
    #make copy of dirichlet nodes
    P = numpy.copy(phi)  
    
    g = numpy.zeros_like(phi)
    dz2 = dz*dz
    dr2 = dr*dr

    rho_e = numpy.zeros_like(phi)
    
    #set radia
    r = numpy.zeros_like(phi)
    for i in range(nz):
        for j in range(nr):
            r[i][j] = R(j)
    
    for it in range (max_it):

        #compute RHS
        #rho_e = QE*n0*numpy.exp(numpy.subtract(phi,phi0)/kTe)
        
        #for i in range(1,nz-1):
        #    for j in range(1,nr-1):
                
        #        if (cell_type[i,j]>0):
        #            continue
                
        #       rho_e=QE*n0*math.exp((phi[i,j]-phi0)/kTe)
        #        b = (rho_i[i,j]-rho_e)/EPS0;
        #        g[i,j] = (b + 
        #                 (phi[i,j-1]+phi[i,j+1])/dr2 +
        #                 (phi[i,j-1]-phi[i,j+1])/(2*dr*r[i,j]) +
        #                 (phi[i-1,j] + phi[i+1,j])/dz2) / (2/dr2 + 2/dz2)
                         
        #        phi[i,j]=g[i,j]

        #compute electron term                                         
        rho_e = QE*n0*numpy.exp(numpy.subtract(P,phi0)/kTe)
        b = numpy.where(cell_type<=0,(rho_i - rho_e)/EPS0,0)

        #regular form inside        
        g[1:-1,1:-1] = (b[1:-1,1:-1] + 
                     (phi[1:-1,2:]+phi[1:-1,:-2])/dr2 +
                      (phi[1:-1,0:-2]-phi[1:-1,2:])/(2*dr*r[1:-1,1:-1]) +
                      (phi[2:,1:-1] + phi[:-2,1:-1])/dz2) / (2/dr2 + 2/dz2)
        
        #neumann boundaries
        g[0] = g[1]       #left
        g[-1] = g[-2]     #right
        g[:,-1] = g[:,-2] #top
        g[:,0] = g[:,1]
        
        #dirichlet nodes
        phi = numpy.where(cell_type>0,P,g)
        
    return phi

#computes electric field                    
def computeEF(phi,efz,efr):
    
    #central difference, not right on walls
    efz[1:-1] = (phi[0:nz-2]-phi[2:nz+1])/(2*dz)
    efr[:,1:-1] = (phi[:,0:nr-2]-phi[:,2:nr+1])/(2*dr)
    
    #one sided difference on boundaries
    efz[0,:] = (phi[0,:]-phi[1,:])/dz
    efz[-1,:] = (phi[-2,:]-phi[-1,:])/dz
    efr[:,0] = (phi[:,0]-phi[:,1])/dr
    efr[:,-1] = (phi[:,-2]-phi[:,-1])/dr
    
def plot(ax,data,scatter=False):
    pl.sca(ax)
    pl.cla()
    cf = pl.contourf(pos_z, pos_r, numpy.transpose(data),8,alpha=.75,linewidth=1,cmap='jet')
    #cf = pl.pcolormesh(pos_z, pos_r, numpy.transpose(data))
    if (scatter):
        ax.hold(True);
        (ZZ,RR)=pl.meshgrid(pos_z,pos_r)
        ax.scatter(ZZ,RR,c=numpy.transpose(cell_type),cmap='jet')
    ax.set_yticks(pos_r)
    ax.set_xticks(pos_z)
    ax.xaxis.set_ticklabels([])
    ax.yaxis.set_ticklabels([])
    pl.xlim(min(pos_z),max(pos_z))
    pl.ylim(min(pos_r),max(pos_r))
    ax.grid(b=True,which='both',color='k',linestyle='-')
    ax.set_aspect('equal', adjustable='box')
 #   pl.colorbar(cf,ax=pl.gca(),orientation='horizontal',shrink=0.75, pad=0.01)

    
#---------- INITIALIZATION ----------------------------------------

pl.close('all')
seed()
       
# allocate memory space
nz = 35
nr = 12
dz = 1e-3
dr = 1e-3    
dt = 5e-9

QE = 1.602e-19
AMU =  1.661e-27
EPS0 = 8.854e-12

charge = QE
m = 40*AMU  #argon ions  
qm = charge/m                   
spwt = 50

#solver parameters
n0 = 1e12
phi0 = 100
phi1 = 0
kTe = 5

phi = numpy.zeros([nz,nr])
efz = numpy.zeros([nz,nr])
efr = numpy.zeros([nz,nr])
rho_i = numpy.zeros([nz,nr])
den = numpy.zeros([nz,nr])

# ---- sugarcube domain --------------------
cell_type = numpy.zeros([nz,nr]);
tube1_radius = 6*dr;
tube1_length = 0.01;
tube1_aperture_rad = 4*dr;
tube2_radius = tube1_radius+dr;
tube2_length = tube1_length+2*dz;
tube2_aperture_rad = 3*dr;
[tube_i_max, tube_j_max] = map(int, XtoL([4*dz, tube1_radius]))

for i in range(0,nz):
    for j in range(0,nr):
        pos = Pos([i,j])  # node position
        
        #inner tube
        if ((i==0 and pos[1]<tube1_radius) or 
        (pos[0]<=tube1_length and pos[1]>=tube1_radius and pos[1]<tube1_radius+0.5*dr) or
        (pos[0]>=tube1_length and pos[0]<tube1_length+0.5*dz and 
            pos[1]>=tube1_aperture_rad and pos[1]<tube1_radius) ) :
            cell_type[i][j]=1
            phi[i][j] = phi0
        
        if ((pos[0]<=tube2_length and pos[1]>=tube2_radius and pos[1]<tube2_radius+0.5*dr) or
        (pos[0]>=tube2_length and pos[0]<=tube2_length+1.5*dz and 
            pos[1]>=tube2_aperture_rad and pos[1]<=tube2_radius) ) :
            cell_type[i][j]=2
            phi[i][j] = phi1
                       
#----------- COMPUTE NODE VOLUMES ------------------------
node_volume = numpy.zeros([nz,nr])
for i in range(0,nz):
    for j in range(0,nr):
        j_min = j-0.5
        j_max = j+0.5
        if (j_min<0): j_min=0
        if (j_max>nr-1): j_max=nr-1
        a = 0.5 if (i==0 or i==nz-1) else 1.0
        #note, this is r*dr for non-boundary nodes
        node_volume[i][j] = a*dz*(R(j_max)**2-R(j_min)**2)  

#create an array of particles
particles = []
    
#counter for fractional particles    
mpf_rem = numpy.zeros([nz,nr])
rho_i = numpy.zeros([nz,nr])


lambda_d = math.sqrt(EPS0*kTe/(n0*QE))
print ("Debye length is %.4g, which is %.2g*dz"%(lambda_d,lambda_d/dz))
print ("Expected ion speed is %.2f m/s"%math.sqrt(2*phi0*qm))

#positions for plotting
pos_r = numpy.linspace(0,(nr-1)*dr,nr)
pos_z = numpy.linspace(0,(nz-1)*dz,nz)
fig1 = pl.figure(num=None, figsize=(20, 10), dpi=80, facecolor='w', edgecolor='k')
sub = (pl.subplot(211),pl.subplot(212))

#solve potential
phi = solvePotential(phi,1000)
computeEF(phi,efz,efr);

#----------- MAIN LOOP --------------------------------------------
for ts in range(0,1000+1):

    den = numpy.zeros([nz,nr])
    
    #compute production rate
    na = 1e15
    ne = 1e12
    k = 2e-10  #not a physical value
    dni=k*ne*na*dt
            
    #inject particles    
    for i in range(1,tube_i_max):
        for j in range(0,tube_j_max):
            
            #skip over solid cells
            if (cell_type[i][j]>0): continue
            
            #interpolate node volume to cell center to get cell volume
            cell_volume = gather(node_volume,(i+0.5,j+0.5))
            
            #floating point production rate
            mpf_new = dni*cell_volume/spwt + mpf_rem[i][j]
            
            #truncate down, adding randomness
            mp_new = int(math.trunc(mpf_new+random()))
            
            #save fraction part
            mpf_rem[i][j] = mpf_new - mp_new    #new fractional reminder
            
            #generate this many particles
            for p in range(mp_new):
                pos = Pos([i+random(), j+random()])
                vel = sampleIsotropicVel(300)                
                particles.append(Particle(pos,vel))
    
    #some arbitrary min value    
    max_zvel = 0
   
    #push particles
    for part in particles:
        #gather electric field
        lc = XtoL(part.pos)
        part_ef = [gather(efz,lc), gather(efr,lc), 0]
        for dim in range(3):
            part.vel[dim] += qm*part_ef[dim]*dt
            part.pos[dim] += part.vel[dim]*dt
        
        #get new maximum velocity, for screen output
        if part.vel[0]>max_zvel: max_zvel = part.vel[0]
                           
        #rotate particle back to ZR plane
        r = math.sqrt(part.pos[1]*part.pos[1] + part.pos[2]*part.pos[2])
        sin_theta_r = part.pos[2]/r
        part.pos[1] = r
        part.pos[2] = 0
        
        #rotate velocity
        cos_theta_r = math.sqrt(1-sin_theta_r*sin_theta_r)
        u2 = cos_theta_r*part.vel[1] - sin_theta_r*part.vel[2]
        v2 = sin_theta_r*part.vel[1] + cos_theta_r*part.vel[2]
        part.vel[1] = u2
        part.vel[2] = v2
        
    #compute density
    p = 0
    np = len(particles)
    while (p<np):
        
        part = particles[p]
        lc = XtoL(part.pos)
        i = int(numpy.trunc(lc[0]))
        j = int(numpy.trunc(lc[1]))
        
        #
        if (i<0 or i>=nz-1 or j>=nr-1 or cell_type[i][j]>0):
            #replace current data with the last entry
            particles[p] = particles[np-1]
            np-=1
            continue
        
        scatter(den, lc, spwt)
        p+=1
       
    #resize particle array
    particles = particles[0:np]
    
    #divide by node volume
    den /= node_volume
    rho_i = charge*den

    #update potential
    phi=solvePotential(phi)
    
    #compute electric field
    computeEF(phi,efz,efr)

    #recompute reference density
    n0 = den.max()    
    
    if (ts % 10==0):
        print ("ts: %d, np: %d, phi range: %.2g:%.2g, max_den: %.3g, max_zvel: %.f"%(ts,len(particles),phi.min(),phi.max(),n0,max_zvel))
     
        #sub = pl.subplot(111,aspect='equal')
        
        #sub[0].hold(False)
        plot(sub[0],numpy.log10(numpy.where(den<=1e4,1e4,den)),scatter=True)
        plot(sub[1],phi)
        pl.draw()
        pl.pause(1e-4) #allow for repaint
      
#----------- END OF MAIN LOOP ------------------------
plot(sub[0],numpy.log10(numpy.where(den<=1e4,1e4,den)),scatter=True)
plot(sub[1],phi)               
#Q = pl.quiver(pos_z, pos_r, numpy.transpose(efz), numpy.transpose(efr),units='xy')
pl.draw()
      

#this will block execution until figure is closed
pl.show()