# HomeWork20
Java Elementary
1. Создать вторую точку входа в приложение (main). Реализовать асинхронную загрузку нескольких изображений, используя сервис Bing, переделав реализацию синхронной загрузки с занятия.
2. Добавить логирование запросов с выводом Thread ID.
3. Измерить время выполнения всех запросов в обоих подходах, используя System.nanoTime.
4. В асинхронной загрузке подменить dispatcher с указанием ExecutorService из стандартных доступных и измерить время выполнения:
  a. Executors.newCachedThreadPool
  b. Executors.newFixedThreadPool
  c. Executors.newSingleThreadExecutor
