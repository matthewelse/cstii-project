// See LICENSE for license details.
package conv

import chisel3._
import chisel3.util.ValidIO

/**
  * Pipelined multiply/accumulate operations to speed up convolutions. This should generalise nicely to 1-dimensional
  * convolutions.
  *
  * Block Diagram (convolutionSize=6):
  *            ┌───┐         ┌───┐         ┌───┐         ┌───┐         ┌───┐
  * p ┈┈┈┈╱┈┈┈┈┤   ├┈┈┈┈╱┈┈┈┈┤   ├┈┈┈┈╱┈┈┈┈┤   ├┈┈┈┈╱┈┈┈┈┤   ├┈┈┈┈╱┈┈┈┈┤   ├┈┈┈┈╱┈┈┐
  * i ┈┈┈┈╱┈┈┈┈┤   ├┈┈┈┈╱┈┈┈┈┤   ├┈┈┈┈╱┈┈┈┈┤   ├┈┈┈┈╱┈┈┈┈┤   ├┈┈┈┈╱┈┈┐ │   │       ┆
  * x ┈┈┈┈╱┈┈┈┈┤   ├┈┈┈┈╱┈┈┈┈┤   ├┈┈┈┈╱┈┈┈┈┤   ├┈┈┈┈╱┈┈┐ │   │       ┆ │   │       ┆
  * e ┈┈┈┈╱┈┈┈┈┤   ├┈┈┈┈╱┈┈┈┈┤   ├┈┈┈┈╱┈┈┐ │   │       ┆ │   │       ┆ │   │       ┆
  * l ┈┈┈┈╱┈┈┈┈┤   ├┈┈┈┈╱┈┈┐ │   │       ┆ │   │       ┆ │   │       ┆ │   │       ┆
  *   ┈┈┈┈╱┈┈┐ │   │       ┆ │   │       ┆ │   │       ┆ │   │       ┆ │   │       ┆
  * i        ┆ │   │       ┆ │   │       ┆ │   │       ┆ │   │       ┆ │   │       ┆
  * n        ┆ └─^─┘       ┆ └─^─┘       ┆ └─^─┘       ┆ └─^─┘       ┆ └─^─┘       ┆
  *          ┆             ┆             ┆             ┆             ┆             ┆
  *         ┌┴┐  ┌─┐      ┌┴┐  ┌─┐      ┌┴┐  ┌─┐      ┌┴┐  ┌─┐      ┌┴┐  ┌─┐      ┌┴┐  ┌─┐
  *     w0┈┈┤*├┈┈┤+├┐ w1┈┈┤*├┈┈┤+├┐ w2┈┈┤*├┈┈┤+├┐ w3┈┈┤*├┈┈┤+├┐ w4┈┈┤*├┈┈┤+├┐ w5┈┈┤*├┈┈┤+├┈┈ pixel_out
  *         └─┘  └┬┘┆     └─┘  └┬┘┆     └─┘  └┬┘┆     └─┘  └┬┘┆     └─┘  └┬┘┆     └─┘  └┬┘
  *               ┆ ┆    ┌─┐    ┆ ┆    ┌─┐    ┆ ┆    ┌─┐    ┆ ┆    ┌─┐    ┆ ┆    ┌─┐    ┆
  *               0 └┈┈┈┈┤ ├┈┈┈┈┘ └┈┈┈┈┤ ├┈┈┈┈┘ └┈┈┈┈┤ ├┈┈┈┈┘ └┈┈┈┈┤ ├┈┈┈┈┘ └┈┈┈┈┤ ├┈┈┈┈┘
  *                      └^┘           └^┘           └^┘           └^┘           └^┘
  * @param convolutionSize total number of weights in each convolution operation.
  * @param pixelSize number of bits
  */
class ConvolutionDataPath(convolutionSize: Int, pixelSize: Int) extends Module {
  val io = IO(new Bundle {
    val pixels_in = Flipped(new ValidIO(Vec(convolutionSize, SInt(pixelSize.W))))
    val pixel_out = new ValidIO(SInt(pixelSize.W))

    val weights = Input(Vec(convolutionSize, SInt(pixelSize.W)))
  })

  // TODO: bundle pixel data and valid bit together
  private val pixelShiftRegister = Seq.tabulate(convolutionSize) (n => {
    RegInit(VecInit(Seq.fill(convolutionSize - n) {
      0.S(pixelSize.W)
    }))
  })

  private val validShiftRegister = RegInit(
    VecInit(Seq.fill(convolutionSize) {
      false.B
    })
  )

  private val macs = Seq.fill(convolutionSize) {
    Module(new MultiplyAccumulator(pixelSize.W, pixelSize.W))
  }

  for (i <- 0 until convolutionSize) {
    pixelShiftRegister.head(i) := io.pixels_in.bits(i)
  }

  macs.head.io.partial := 0.S
  validShiftRegister(0) := io.pixels_in.valid

  // Setup the data path
  for (i <- 0 until convolutionSize) {
    // TODO: how to handle changing the weights?
    macs(i).io.weight := io.weights(i)
    macs(i).io.pixel <> pixelShiftRegister(i)(0)

    if (i != convolutionSize - 1) {
      macs(i).io.partial_out <> macs(i + 1).io.partial
      validShiftRegister(i + 1) := validShiftRegister(i)

      // connect up the pixel shift register
      for (j <- 0 until (convolutionSize - i - 1)) {
        pixelShiftRegister(i + 1)(j) := pixelShiftRegister(i)(j + 1)
      }
    } else {
      io.pixel_out.bits <> macs(i).io.partial_out
      io.pixel_out.valid := validShiftRegister(convolutionSize - 1)
    }
  }
}
