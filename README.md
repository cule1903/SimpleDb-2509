# SimpleDb JDBC

순수 JDBC로 MySQL을 다루는 경량 DB 유틸리티 과제입니다.  
`SimpleDb`는 커넥션과 트랜잭션을 관리하고, `Sql`은 SQL 작성과 실행을 담당합니다.

## 개발 환경

- Java 21
- Gradle
- MySQL
- JUnit 5
- AssertJ
- Jackson
- IntelliJ IDEA

## 실행 준비

MySQL Docker 컨테이너를 실행합니다.

```bash
docker run \
  --name mysql-1 \
  -p 3306:3306 \
  -e TZ=Asia/Seoul \
  -e MYSQL_ROOT_PASSWORD=root123414 \
  -d \
  mysql
```

테스트용 DB를 생성합니다.

```bash
docker exec -it mysql-1 mysql -uroot -proot123414
```

```sql
CREATE DATABASE IF NOT EXISTS simpleDb__test;
```

## 테스트 실행

```bash
./gradlew test
```

IntelliJ에서는 프로젝트를 연 뒤 `SimpleDbTest`를 실행하면 됩니다.

## 주요 기능

- SQL 실행: `run`
- SQL 빌더: `append`, `appendIn`
- 데이터 변경: `insert`, `update`, `delete`
- 단일 값 조회: `selectLong`, `selectString`, `selectBoolean`, `selectDatetime`
- 행 조회: `selectRow`, `selectRows`
- 객체 매핑: `selectRow(Class<T>)`, `selectRows(Class<T>)`
- 트랜잭션: `startTransaction`, `commit`, `rollback`
- 스레드별 커넥션 관리: `ThreadLocal<Connection>`

## 프로젝트 구조

```txt
src/main/java/com/back/
├── Article.java
└── simpleDb/
    ├── SimpleDb.java
    └── Sql.java

src/test/java/com/back/simpleDb/
└── SimpleDbTest.java
```

## 회고

처음에는 JDBC와 Connection 개념이 낯설어서 흐름을 이해하는 데 시간이 걸렸습니다.  
테스트 코드를 하나씩 보면서 필요한 기능을 구현하니 어떤 기능이 왜 필요한지 조금씩 이해할 수 있었습니다.  
아직 부족하지만 직접 에러를 고치면서 DB 연결, SQL 실행, 트랜잭션의 기본 구조를 배울 수 있었습니다.
