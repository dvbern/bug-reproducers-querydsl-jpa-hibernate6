package org.example;

import com.querydsl.jpa.impl.JPAQuery;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.example.QMyEntity.myEntity;

@DisplayNameGeneration(DisplayNameGenerator.IndicativeSentences.ReplaceUnderscores.class)
public class QueryProjectionTypeBugTest {

	private static final EntityManagerFactory EMF = Persistence.createEntityManagerFactory("test");

	// instantiate only once for easier debugging
	public static final MyCustomNumber MY_CUSTOM_NUMBER = new MyCustomNumber("111");

	private final EntityManager em = EMF.createEntityManager();
	private final MyEntity aaa111 = new MyEntity(
			"00000000-0000-0000-0000-000000000001",
			"aaa",
			MY_CUSTOM_NUMBER
	);


	@BeforeEach
	void beforeEach() {
		em.getTransaction().begin();

		em.persist(aaa111);

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
	void plain_entity_path_query_works_as_expected() {
        MyCustomNumber actual = new JPAQuery<>(em)
				.select(myEntity.myCustomNumber)
				.from(myEntity)
				.where(myEntity.id.eq(aaa111.getId()))
				.fetchOne();

		assertThat(actual)
				.isEqualTo(aaa111.getMyCustomType());
	}

	@Test
	void simple_projection_query_works_as_expected() {
        MyProjection actual = new JPAQuery<>(em)
				.select(new QMyProjection(myEntity.type, myEntity.myCustomNumber))
				.from(myEntity)
				.fetchOne();

		assertThat(actual)
				.isEqualTo(new MyProjection("aaa", MY_CUSTOM_NUMBER));
	}

	@Test
	void query_with_NumberExpression_fails() {
        MyCustomNumber actual = new JPAQuery<>(em)
				.select(myEntity.myCustomNumber.sumAggregate())
				.from(myEntity)
				.fetchOne();

		// instead throws:
		// java.lang.IllegalArgumentException: Unsupported target type : MyCustomNumber

		assertThat(actual)
				.isEqualTo(MY_CUSTOM_NUMBER);
	}

}
