
import matplotlib
matplotlib.use('QtAgg')

import random
import math
from matplotlib.patches import Circle
import matplotlib.pyplot as plt

from classes.particle import Particle
    

def generate_particles(num_particles, bounds, radius_range, max_attempts=5000):
    xmin, xmax, ymin, ymax = bounds
    rmin, rmax = radius_range
    particles = []

    attempts = 0
    while len(particles) < num_particles and attempts < max_attempts:
        attempts += 1
        r = random.uniform(rmin, rmax)
        # Keep center inside boundary so the circle doesn't clip
        x = random.uniform(xmin + r, xmax - r)
        y = random.uniform(ymin + r, ymax - r)
        
        new_particle = Particle(x, y, r)
        
        # Ensure no collision with existing particles
        if not any(new_particle.collides_with(p) for p in particles):
            particles.append(new_particle)
            
    return particles

def plot_interactive_particles(particles, bounds):
    fig, ax = plt.subplots(figsize=(8, 8))
    xmin, xmax, ymin, ymax = bounds
    ax.set_xlim(xmin, xmax)
    ax.set_ylim(ymin, ymax)
    ax.set_aspect('equal')
    ax.set_title("Click any particle to display details", fontsize=12)

    # Use Circle directly here instead of plt.Circle
    for p in particles:
        circle = Circle((p.x, p.y), p.r, color=random_color(), alpha=0.7, ec='black', lw=1.5)
        ax.add_patch(circle)

    # Tooltip annotation
    annot = ax.annotate(
        "", xy=(0, 0), xytext=(15, 15), textcoords="offset points",
        bbox=dict(boxstyle="round,pad=0.5", fc="white", ec="black", alpha=0.9),
        arrowprops=dict(arrowstyle="->", connectionstyle="arc3,rad=0")
    )
    annot.set_visible(False)

    def on_click(event):
        if event.inaxes != ax or event.xdata is None or event.ydata is None:
            return

        clicked_particle = None
        for p in particles:
            dist = math.hypot(event.xdata - p.x, event.ydata - p.y)
            if dist <= p.r:
                clicked_particle = p
                break

        if clicked_particle:
            annot.xy = (clicked_particle.x, clicked_particle.y)
            annot.set_text(
                f"x: {clicked_particle.x:.2f}\n"
                f"y: {clicked_particle.y:.2f}\n"
                f"r: {clicked_particle.r:.2f}"
            )
            annot.set_visible(True)
        else:
            annot.set_visible(False)

        fig.canvas.draw_idle()

    fig.canvas.mpl_connect("button_press_event", on_click)
    plt.show()
    
def random_color():
    return (random.random(), random.random(), random.random())

# --- Configuration ---
if __name__ == "__main__":
    BOUNDS = (0, 100, 0, 100)       # (xmin, xmax, ymin, ymax)
    RADIUS_RANGE = (2.0, 7.0)       # Min and Max radius
    NUM_PARTICLES = 50              # Number of particles

    particle_list = generate_particles(NUM_PARTICLES, BOUNDS, RADIUS_RANGE)
    plot_interactive_particles(particle_list, BOUNDS)