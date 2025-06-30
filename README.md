#UNC Head Volume Rendering (Java)

This project visualizes the UNC Head dataset using a custom-built volume rendering engine developed entirely in Java. The aim was to recreate a reference grayscale image by implementing core concepts in volume rendering, 3D interpolation, and 2D image synthesis without relying on external graphics libraries.

The dataset is a 3D scalar field of size 256 × 256 × 225 (width × height × depth), where each voxel represents an intensity value from a medical scan. Rendering is achieved through ray casting, where rays are projected orthogonally along the z-axis. As each ray passes through the volume, intensity values are sampled and blended using alpha compositing to generate a final grayscale image.

To enhance image quality, the renderer uses trilinear interpolation, enabling smooth transitions between voxel values. An intensity window of 62–120 is applied to isolate relevant anatomical structures and simulate an X-ray–like appearance. The implementation also supports resolution-independent rendering, allowing images to be generated at custom sizes while maintaining detail and contrast.

Key Features:-
Processes volumetric scan data (UNC Head)

Implements ray casting and alpha blending

Uses trilinear interpolation for smoother results

Applies orthographic projection from a fixed view

Generates grayscale output at arbitrary resolutions

Built entirely with standard Java libraries

Learning Outcomes:-
Through this project, I developed practical knowledge in:

Volume rendering techniques

Interpolating scalar fields in 3D space

Image generation through projection

Efficient CPU-based rendering without third-party tools

Limitations:-
Only orthographic projection (no interactive views)

Static rendering from a fixed direction

Performance may be limited at high resolutions due to CPU-only implementation

This project demonstrates an end-to-end rendering solution for 3D medical imaging data, built from the ground up with a focus on interpolation, accuracy, and visual fidelity using core programming principles.
