# ednlang ![Clojure CI](https://github.com/fvm-project/ednlang/workflows/Clojure%20CI/badge.svg)

**ednlang** is a simple stack-based concatenative language implemented using [fvm](https://github.com/fvm-project/fvm).

## Example

Here's a ednlang program that calculates and prints `factorial(5)`:
```clojure
;; import the standard library
{::fvm/type ::requires
 ::value ["lib/std.edn"]}

;; define a new opcode for factorial
{::fvm/type ::defop
 ::name :test/fact
 ::value [{::fvm/type ::push
           ::value 0}
          {::fvm/type ::eq?
           ::then [{::fvm/type ::pop}
                   {::fvm/type ::pop}
                   {::fvm/type ::push
                    ::value 1}]
           ::else [{::fvm/type ::pop}
                   {::fvm/type ::dup}
                   {::fvm/type ::dec}
                   {::fvm/type :test/fact}
                   {::fvm/type ::mul}]}]}

;; call it
{::fvm/type ::push
 ::value 5}
{::fvm/type :test/fact}

;; print the result
{::fvm/type ::println}
```

## Usage

### JVM

To run the example factorial program, do:
```
$ lein run test/fact.edn
```

### Native

#### Build from source

Make sure you have GraalVM installed and `$GRAALVM_HOME` pointing to it, then do:
```
$ ./compile
```

#### Run

Now you can do:
```
$ target/ednlang test/fact.edn
```

## Tests

```
$ lein eftest
```

## Properties

- Simple stack-based language
- Custom ops (like `fact` above) are inlined and called at runtime
- No recursion limit - try running the factorial program for large values
- Code is data is code - anonymous ops can be stored and called
- Does not require a GC, being completely stack based

## Status

This is a research project with rough edges - here be dragons.

## License

Copyright © 2020 Divyansh Prakash

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
