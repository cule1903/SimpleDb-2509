# SimpleDb JDBC 과제

순수 JDBC로 만든 경량 MySQL 유틸리티입니다. `SimpleDb`는 커넥션과 트랜잭션을 관리하고, `Sql`은 SQL 조립, 파라미터 바인딩, 실행, 조회 결과 매핑을 담당합니다.

## 필요한 개발 환경

- JDK 21
- Gradle 또는 IntelliJ IDEA의 Gradle 실행 기능
- Docker Desktop
- MySQL Docker 컨테이너
- IDE 추천: IntelliJ IDEA

현재 터미널에는 `gradle` 명령이 설치되어 있지 않습니다. IntelliJ에서 프로젝트를 열면 Gradle을 자동 인식해서 테스트를 실행할 수 있고, 터미널에서 실행하려면 Gradle을 설치하거나 Gradle Wrapper를 추가해야 합니다.

## IntelliJ에서 열기

1. IntelliJ IDEA 실행
2. `Open` 선택
3. 아래 폴더 선택

```txt
/Users/apple/Documents/Codex/simple-db-jdbc
```

4. Gradle 프로젝트로 import될 때까지 기다리기
5. Project SDK를 Java 21로 설정
   - `File > Project Structure > Project > SDK`
6. `SimpleDbTest` 파일을 열고 테스트 실행

테스트 파일 위치:

```txt
src/test/java/com/back/simpleDb/SimpleDbTest.java
```

## MySQL 실행

과제 안내에 맞춰 macOS에서는 `-v` 옵션 없이 실행합니다.

```bash
docker run \
  --name mysql-1 \
  -p 3306:3306 \
  -e TZ=Asia/Seoul \
  -e MYSQL_ROOT_PASSWORD=root123414 \
  -d \
  mysql
```

이미 같은 이름의 컨테이너가 있으면 다음 명령으로 시작합니다.

```bash
docker start mysql-1
```

## 테스트 DB 생성

```bash
docker exec -it mysql-1 mysql -uroot -proot123414
```

MySQL 콘솔에서:

```sql
CREATE DATABASE IF NOT EXISTS simpleDb__test;
```

## 테스트 실행

Gradle이 설치되어 있다면:

```bash
gradle test
```

IntelliJ를 사용한다면:

1. `simple-db-jdbc` 폴더를 프로젝트로 열기
2. Gradle import 완료 기다리기
3. `src/test/java/com/back/simpleDb/SimpleDbTest.java` 실행

## 구현 구조

```txt
src/main/java/com/back/
├── Article.java                 # 테스트용 DTO
└── simpleDb/
    ├── SimpleDb.java            # 커넥션, 트랜잭션, 기본 SQL 실행
    └── Sql.java                 # SQL 빌더, CRUD, 조회, DTO 매핑

src/test/java/com/back/simpleDb/
└── SimpleDbTest.java            # t001 ~ t019 테스트
```

## 핵심 구현 포인트

- `ThreadLocal<Connection>`으로 스레드별 커넥션을 분리했습니다.
- 같은 스레드에서는 `close()` 전까지 같은 커넥션을 재사용합니다.
- `startTransaction()`, `commit()`, `rollback()`은 현재 스레드의 커넥션을 기준으로 동작합니다.
- `append(...)`는 SQL 조각과 바인딩 값을 순서대로 저장합니다.
- `appendIn(...)`은 `IN (?)`, `FIELD(id, ?)` 같은 SQL에서 `?`를 값 개수만큼 펼칩니다.
- `selectRows()`는 `List<Map<String, Object>>`로 조회 결과를 반환합니다.
- `selectRows(Article.class)`처럼 DTO 클래스를 넘기면 Jackson으로 객체 매핑합니다.
- MySQL `DATETIME`은 `LocalDateTime`, `BIT(1)`은 `Boolean`, 정수 값은 `Long` 중심으로 정규화합니다.
