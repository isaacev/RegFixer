# RegFixer

On macOS with homebrew run `brew install jflex` to install Jflex.

Run `make all` to build the lexer, parser, and RegFixer libraries.

Running `make run` should produce the following results:

```
regex:              twig:
(ab)*c(d)+          (.)*
----------------------------------------
(🔮 b)*c(d)+        ((.)*b)*c(d)+
(a🔮 )*c(d)+        (a(.)*)*c(d)+
(🔮 )*c(d)+         ((.)*)*c(d)+
🔮 c(d)+            (.)*c(d)+
(ab)*🔮 (d)+        (ab)*(.)*(d)+
(ab)*c(🔮 )+        (ab)*c((.)*)+
(ab)*c🔮            (ab)*c(.)*
🔮 (d)+             (.)*(d)+
(ab)*🔮             (ab)*(.)*
🔮                  (.)*

regex:              twig:
(ab)*c(d)+          (\w)+
----------------------------------------
(🔮 b)*c(d)+        ((\w)+b)*c(d)+
(a🔮 )*c(d)+        (a(\w)+)*c(d)+
(🔮 )*c(d)+         ((\w)+)*c(d)+
🔮 c(d)+            (\w)+c(d)+
(ab)*🔮 (d)+        (ab)*(\w)+(d)+
(ab)*c(🔮 )+        (ab)*c((\w)+)+
(ab)*c🔮            (ab)*c(\w)+
🔮 (d)+             (\w)+(d)+
(ab)*🔮             (ab)*(\w)+
🔮                  (\w)+

regex:              twig:
(ab)*c(d)+          [0-9]
----------------------------------------
(🔮 b)*c(d)+        ([0-9]b)*c(d)+
(a🔮 )*c(d)+        (a[0-9])*c(d)+
(🔮 )*c(d)+         ([0-9])*c(d)+
🔮 c(d)+            [0-9]c(d)+
(ab)*🔮 (d)+        (ab)*[0-9](d)+
(ab)*c(🔮 )+        (ab)*c([0-9])+
(ab)*c🔮            (ab)*c[0-9]
🔮 (d)+             [0-9](d)+
(ab)*🔮             (ab)*[0-9]
🔮                  [0-9]
```
