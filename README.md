# SOCKS5 proxy server

Welcome to the SOCKS5proxy.
There are code for the SOCKS5 proxy server on Java.
Date: 26.12.2021

## Execution

Program needs one program-arg: port for starting server.

## Connection

1. curl --socks5-hostname localhost:1026 https://www.google.com/

2. Set SOCKS5 proxy in the Firefox browser.
![Firefox settings](https://i.postimg.cc/cLHySzvW/Screenshot-from-2021-12-26-13-41-28.png)


# Task SOCKS-прокси
1. Необходимо реализовать прокси-сервер, соответствующий стандарту SOCKS версии 5.
2. В параметрах программе передаётся только порт, на котором прокси будет ждать входящих подключений от клиентов.
3. Из трёх доступных в протоколе команд, обязательной является только реализация команды 1 (establish a TCP/IP stream connection)
4. Поддержку аутентификации и IPv6-адресов реализовывать не требуется.
5. Для реализации прокси использовать неблокирующиеся сокеты, работая с ними в рамках одного треда. Дополнительные треды использовать не допускается. Соответственно, никаких блокирующихся вызовов (кроме вызова селектора) не допускается.
6. Прокси не должна делать предположений о том, какой протокол уровня приложений будет использоваться внутри перенаправляемого TCP-соединения. В частности, должна поддерживаться передача данных одновременно в обе стороны, а соединения должны закрываться аккуратно (только после того, как они больше не нужны).
7. В приложении не должно быть холостых циклов ни в каких ситуациях. Другими словами, не должно быть возможно состояние программы, при котором неоднократно выполняется тело цикла, которое не делает ни одной фактической передачи данных за итерацию.
8. Не допускается неограниченное расходование памяти для обслуживания одного клиента.
9. Производительность работы через прокси не должна быть заметно хуже, чем без прокси. Для отслеживания корректности и скорости работы можно глядеть в Developer tools браузера на вкладку Network.
Прокси должен поддерживать резолвинг доменных имён (значение 0x03 в поле address). Резолвинг тоже должен быть неблокирующимся. 
Для этого предлагается использовать следующий подход:
  - На старте программы создать новый UDP-сокет и добавить его в селектор на чтение
  - Когда необходимо отрезолвить доменное имя, отправлять через этот сокет DNS-запрос A-записи на адрес рекурсивного DNS-резолвера
  - В обработчике чтения из сокета обрабатывать случай, когда получен ответ на DNS-запрос, и продолжать работу с полученным адресом
  - Для получения адреса рекурсивного резолвера, а также для формирования и парсинга DNS-сообщений на Java предлагается использовать библиотеку dnsjava     (для других языков найдите сами).
10. Tестирования можно настроить любой Web-браузер на использование вашего прокси, и посещать любые веб-сайты, богатые контентом.
