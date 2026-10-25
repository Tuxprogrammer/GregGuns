# Third-party notices

GregGuns is MIT-licensed (see [LICENSE](LICENSE)). This file lists the third-party code that GregGuns ports or adapts, with the license notice each license requires us to keep. Third-party assets are listed in [docs/assets/CREDITS.md](docs/assets/CREDITS.md) and live in the third-party asset folder, each beside its license text.

The rules for porting are in [CONTRIBUTING § Ported code (MIT)](CONTRIBUTING.md#ported-code-mit). `PortedCodeGateTest` fails the build when a ported source file has no entry here.

## ICBM-Classic (MIT)

- **Project:** ICBM-Classic by Built Broken Modding
- **Repository:** https://github.com/BuiltBrokenModding/ICBM-Classic
- **Branch and commit:** `prod/1.12` at `9c1d985a375adc2868ff47707b3a636452d0b63a` (every ported file's header links to this commit)
- **License:** MIT, which applies to code only. ICBM-Classic's assets have their own license and are not used here.
- **Used for:** the algorithms that the ICBM spec marks `MIT-portable`, rewritten in GregGuns style. Nothing is taken from ICBM-Classic's `prod/1.7` branch, which has no license.

```
MIT License

Copyright (c) 2023 Built Broken Modding

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
