// See LICENSE for license details.
package conv

import chisel3.{Driver, Module}
import java.io.{File, PrintWriter}

import firrtl.{CircuitState, ChirrtlForm, VerilogCompiler}

object Builder {
  def build(name: String, device: (() => Module)): Unit = {
    // find the build directory
    val dir = new File("./build").getCanonicalFile
    dir.mkdirs()

    // parse rtl
    val drv = Driver.emit(device)
    val nets = firrtl.Parser.parse(drv)

    val verilogOut = dir.toPath.resolve(s"${name}.v").toString
    val writer = new PrintWriter(verilogOut)
    val compiler = new VerilogCompiler

    val result = compiler.compileAndEmit(CircuitState(nets, ChirrtlForm))

    writer.write(result.getEmittedCircuit.value)
    writer.close()
  }
}

object Main {
  def main(args: Array[String]): Unit = {
    Builder.build("simple_convolution", { () => new SimpleConvolution })
  }
}
