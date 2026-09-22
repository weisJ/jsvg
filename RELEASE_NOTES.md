## Changes

* Support for `<feTile>`.
* Correct filter primitive subregions and clipping, including operations that make
  transparent input pixels visible.
* Improved transform handling for filters, including rotated and sheared blurs,
  offsets, displacement maps and turbulence.
* Fixed Gaussian blur edge modes and premature clipping of drop shadows.
* Honor `color-interpolation-filters` in compositing, Gaussian blur, displacement maps
  and turbulence.
* Corrected overlay, hard-light, color-dodge, color-burn, exclusion and nonseparable
  blend modes, including translucent inputs.
* Improved `<feDiffuseLighting>` surface normals, sampling and performance.
* Fixed `<feComponentTransfer>` alpha tables with `color-interpolation-filters="linearRGB"`
  and `<feFlood>` color resolution for animation and `currentColor`.
* Fixed missing filter input lookup, reused result names and empty `<feMerge>` output.
* Fixed filter positioning and bounds for nested SVGs, `<use>` and `<image>` elements,
  and reference point positioning and clipping for symbols and markers.
* Fixed `non-scaling-stroke` with transformed output graphics and filter buffers.
