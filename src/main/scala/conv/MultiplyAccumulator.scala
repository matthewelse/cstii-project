// See LICENSE for license details.
package conv

import chisel3._
import chisel3.internal.firrtl.Width

class MultiplyAccumulator(pixel_w: Width, acc_w: Width) extends Module {
  val io = IO(new Bundle {
    val weight = Input(SInt(pixel_w))
    val pixel = Input(SInt(pixel_w))

    // TODO: pick a larger size to store partial sums
    val partial = Input(SInt(acc_w))
//    val zero = Input(Bool())

    val partial_out = Output(SInt(acc_w))
  })

  val sum = RegInit(0.S(8.W))

  sum := (io.weight * io.pixel) + io.partial

  io.partial_out := sum
}
