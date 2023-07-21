package org.example;

import com.querydsl.jpa.impl.JPAQuery;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.example.QMyEntity.myEntity;

public class QueryProjectionTypeBugTest {

	private static final EntityManagerFactory EMF = Persistence.createEntityManagerFactory("test");

	private final EntityManager em = EMF.createEntityManager();
	private final MyEntity aaa111 = new MyEntity(
			"00000000-0000-0000-0000-000000000001",
			"aaa",
			new MyCustomNumber("111")
	);
	private final MyEntity aaa222 = new MyEntity(
			"00000000-0000-0000-0000-000000000002",
			"aaa",
			new MyCustomNumber("222")
	);
	private final MyEntity zzz444 = new MyEntity(
			"00000000-0000-0000-0000-000000000004",
			"zzz",
			new MyCustomNumber("444")
	);
	private final MyEntity zzz555 = new MyEntity(
			"00000000-0000-0000-0000-000000000005",
			"zzz",
			new MyCustomNumber("555")
	);

	@BeforeEach
	void beforeEach() {
		em.getTransaction().begin();

		em.persist(aaa111);
		em.persist(aaa222);
		em.persist(zzz444);
		em.persist(zzz555);

		em.flush();
	}

	@AfterEach
	void afterEach() {
		em.getTransaction().rollback();

		em.clear();
		em.close();
	}

	@AfterAll
	static void afterAll() {
		EMF.close();
	}

	@Test
	void plain_queries_call_the_converters() {
		var rows = new JPAQuery<>(em)
				.select(myEntity)
				.from(myEntity)
				.fetch();

		// works as expected, values
		assertThat(rows)
				.usingElementComparator(MyEntity.comparator())
				.containsExactlyInAnyOrder(
						aaa111,
						aaa222,
						zzz444,
						zzz555
				);
	}

	@Test
	void simple_projection_queries_work() {
		var rows = new JPAQuery<>(em)
				.select(new QMyProjection(myEntity.type, myEntity.myCustomNumber))
				.from(myEntity)
				.fetch();

		assertThat(rows)
				.containsExactlyInAnyOrder(
						new MyProjection("aaa", new MyCustomNumber("111")),
						new MyProjection("aaa", new MyCustomNumber("222")),
						new MyProjection("zzz", new MyCustomNumber("444")),
						new MyProjection("zzz", new MyCustomNumber("555"))
				);
	}

	@Test
	void projection_queries_with_sum_fail() {
		var rows = new JPAQuery<>(em)
				.select(new QMyProjection(myEntity.type, myEntity.myCustomNumber.sum()))
				.from(myEntity)
				.groupBy(myEntity.type)
				.fetch();

		// instead throws:
		// java.lang.IllegalArgumentException: Unsupported target type : MyCustomNumber

		assertThat(rows)
				.containsExactlyInAnyOrder(
						new MyProjection("aaa", new MyCustomNumber("333")),
						new MyProjection("zzz", new MyCustomNumber("999"))
				);
	}

}
