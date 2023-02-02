import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * @for Tema2 - APD - Java Threading
 * @author Dumitrescu Alexandra
 * @since Dec 2022
 * <>
 *     2nd Layer Threads - The products Processors
 *
 *     Logic:
 *          Each thread is given an order id and an index for
 *          the corresponding product to search and an order
 *          index for the vector of semaphores.
 *
 *          [1.] Read each line and search for the <order_id> order.
 *          [2.] Read until the <product_index> product is found and shipped
 * </>
 * */

public class Layer_2Worker implements Runnable {
	/** Order id */
	private String order_id;
	/** Order index used for the shared vector of semaphores */
	private int order_index;
	/** Product index in the given order */
	private int product_index;

	/** Constructor */
	public Layer_2Worker(String order_id, int order_index, int product_index) {
		this.order_id = order_id;
		this.order_index = order_index;
		this.product_index = product_index;
	}

	@Override
	public void run() {
		try {
			/** Open the file reader */
			BufferedReader reader = new BufferedReader(new FileReader(Tema2.input_products_file));

			String product, product_order_id = null, product_id = null;
			/** Counter used for counting the products until the searched one is reached */
			int counter = 0;

			while((product = reader.readLine()) != null) {
				/** Pare each line */
				String[] ids = product.split(",");
				/** Get order ID */
				product_order_id = ids[0];
				/** Get product ID */
				product_id = ids[1];

				/** Check if the parsed order ID is equal to the searched one */
				if(product_order_id.compareTo(order_id) == 0) {
					/** Stop at the product_index product in the order */
					if(counter == product_index) {
						break;
					}
					counter ++;
				}
			}
			/** Close the reader */
			reader.close();

			/** Make sure only one 2nd layer thread writes in the output file at a time */
			Tema2.writeEnableProduct.acquire();
			Tema2.product_writer.append(order_id + "," + product_id + "," + "shipped\n");
			Tema2.writeEnableProduct.release();

			/** Increment the number of tasks in the pool */
			Tema2.inQueue.decrementAndGet();
		} catch (FileNotFoundException e) {
			System.out.println("File not found");
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		} finally {
			/** Stop the service once all tasks are finished and all 1st layer threads
			 *  have added tasks in the service */


			if(Tema2.inQueue.get() == 0 && Tema2.counter == Tema2.maximum_threads) {
				Tema2.service.shutdown();
			}
			/** Decrement the semaphore */
			Tema2.semaphores[order_index].release();
		}
	}
}
