import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.Semaphore;


/**
 * @for Tema2 - APD - Java Threading
 * @author Dumitrescu Alexandra
 * @since Dec 2022
 *
 * <>
 *     1st Layer Threads - The commands Processors
 *
 *     Logic:
 *          [1.] These threads are meant to process the orders in
 *          the orders.txt file.
 *          [2.] For each order add in the executor service <number_of_products> of
 *          the order in the service. The 2nd layer threads will search for
 *          the corresponding products of the order.
 *          [3.] The commands should be equally split. For this idea, we used
 *          the logic previously described with splitting the number of bytes
 *          of the input file. Each thread jump to its corresponding chunk of bytes.
 * </>
 * */
public class Layer_1Worker extends Thread {
	/** ID of a thread */
	private int id;

	/** Constructor */
	public Layer_1Worker(int id) {
		this.id = id;
	}

	/** Announce when the thread had finished adding tasks */
	boolean last = false;

	public void run() {

		/** Split the total number of bytes in equal <maximum_threads> chunks */
		long start_order = (long) (id * ((double)Tema2.input_file_bytes / (double) Tema2.maximum_threads));
		long end_order = (long) Math.min(Tema2.input_file_bytes, (id + 1) *
				((double) Tema2.input_file_bytes / (double) Tema2.maximum_threads));

		try {


			/** Open the input file */
			RandomAccessFile reader = new RandomAccessFile(Tema2.input_orders_file, "r");

			/** Jump to the start of the corresponding chunk of bytes */
			reader.seek(start_order);

			if(start_order != 0) {
				reader.seek(start_order - 1);
				byte prev_character = reader.readByte();
				if(prev_character != '\n') {
					reader.seek(start_order);
					byte first_character = reader.readByte();

					/** If the start is in the middle, read until the next available line */
					while(first_character != '\n' && (reader.getFilePointer() < Tema2.input_file_bytes)) {
						first_character = reader.readByte();
					}
				}
			}


			/** Read each line until we get to the end of the chunk of bytes or to the end of the file */

			while(true) {
				/** Read line */
				String line = reader.readLine();

				if(reader.getFilePointer() >= end_order
						|| line == null
						|| (reader.getFilePointer() >= Tema2.input_file_bytes)) {
					last = true;
				}

				/** Parse line and process given order */
				process_order(line);


				if(reader.getFilePointer() >= end_order
						|| line == null
						|| (reader.getFilePointer() >= Tema2.input_file_bytes)) {
					break;
				}

			}

			/** At last, close the file reader */
			reader.close();


		} catch (FileNotFoundException e) {
			System.out.println("file not found");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}



	private void process_order(String order)
	{
		String[] order_components = order.split(",");
		/** Get the order id */
		String order_id = order_components[0];
		/** Get the number of products */
		int number_of_components = Integer.parseInt(order_components[1]);
		/** Initialize the semaphore
		 *
		 *         Suppose we have thread 1 wanting to ship the command <o_example, 3>
		 *         It parses the line and adds the following tasks:
		 *             <o_example, 1> [Search for 1st product from order o_example]
		 *             <o_example, 2> [Search for 2nd product from order o_example]
		 *             <o_example, 3> [Search for 3rd product from order o_example]
		 *         We initialize
		 *             semaphore[1] = -3 + 1 [Meaning that only when all 3 threads
		 *             found the 3 products and released the semaphore the tasks are
		 *             shipped and the main thread can add the shipped order]
		 * */
		Tema2.semaphores[id] = new Semaphore(-number_of_components + 1);

		/** For each component of the order add a new task in the pool
		 *  for the 2nd layer threads */

		for(int i = 0; i < number_of_components; i++) {
			Tema2.inQueue.incrementAndGet();
			Tema2.service.submit(new Layer_2Worker(order_id, id, i));
		}

		/**
		 * We will stop the executor service once all 1st layer threads have
		 * added all tasks in the pool. This way, we use a counter
		 * to keep track of threads.
		 * */
		if(last) {
			try {
				Tema2.incrementCounterEnable.acquire();
				Tema2.counter ++;
				Tema2.incrementCounterEnable.release();
			} catch (Exception e) {

			}
		}

		/** Check if dummy order */
		if(number_of_components != 0) {
			try {
				/** Wait until all products are shipped */
				Tema2.semaphores[id].acquire();

				/** Write the result in the output file */
				Tema2.writeEnableOrder.acquire();
				Tema2.order_writer.append(order_id + "," + number_of_components + ",shipped\n");
				Tema2.writeEnableOrder.release();
			} catch (Exception e) {
			}
		}
	}



}
